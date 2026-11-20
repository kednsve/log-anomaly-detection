import argparse
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.metrics import (
    average_precision_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)

from config import (
    CLEANED_DIR,
    DEFAULT_CONTAMINATION,
    MODEL_DIR,
    MODEL_PATH,
    N_ESTIMATORS,
    RANDOM_STATE,
)
from core.features import FEATURE_COLUMNS, build_features


def load_features(path: Path) -> pd.DataFrame:
    """从磁盘读取一个csv，聚合特征"""
    df = pd.read_csv(path)
    return build_features(df)


def make_model(contamination: float) -> IsolationForest:
    return IsolationForest(
        n_estimators=N_ESTIMATORS,
        contamination=contamination,
        random_state=RANDOM_STATE,
        n_jobs=-1,
    )


def evaluate_holdout(stem, train_features, demo_features, truth, contamination):
    """用其余文件的 train 训练，在 stem 的 demo 上评估。"""
    train_X = pd.concat(
        [features for name, features in train_features.items() if name != stem],
        ignore_index=True,
    )

    test = demo_features[stem].merge(
        truth[stem][["ip", "is_attack", "attack_type"]],
        on="ip",
        how="inner",
    ).reset_index(drop=True)
    test["attack_type"] = test["attack_type"].fillna("未知")

    model = make_model(contamination)
    model.fit(train_X[FEATURE_COLUMNS])

    y_true = test["is_attack"].to_numpy()
    y_pred = (model.predict(test[FEATURE_COLUMNS]) == -1).astype(int)
    score = -model.decision_function(test[FEATURE_COLUMNS])
    has_both = len(np.unique(y_true)) > 1

    # 只统计攻击 IP 的分类型检出情况（模型不预测类型，只判断是否异常）
    test["pred"] = y_pred
    type_stats = {}
    attack_rows = test[test["is_attack"] == 1]
    for attack_type, group in attack_rows.groupby("attack_type"):
        type_stats[str(attack_type)] = {
            "detected": int(group["pred"].sum()),
            "total": int(len(group)),
        }

    metrics = {
        "stem": stem,
        "ips": len(test),
        "attacks": int(y_true.sum()),
        "precision": precision_score(y_true, y_pred, zero_division=0),
        "recall": recall_score(y_true, y_pred, zero_division=0),
        "f1": f1_score(y_true, y_pred, zero_division=0),
        "auc": roc_auc_score(y_true, score) if has_both else float("nan"),
        "pr_auc": average_precision_score(y_true, score) if has_both else float("nan"),
        "type_stats": type_stats,
    }
    return metrics, confusion_matrix(y_true, y_pred, labels=[0, 1])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--contamination", type=float, default=DEFAULT_CONTAMINATION)
    parser.add_argument("--holdout", default=None, help="只评估指定文件，默认评估全部")
    args = parser.parse_args()

    datasets = {
        p.name.removesuffix("-train.csv"): {
            "train": p,
            "demo": p.with_name(p.name.replace("-train.csv", "-demo.csv")),
            "truth": p.with_name(p.name.replace("-train.csv", "-truth.csv")),
        }
        for p in sorted(CLEANED_DIR.glob("*-train.csv"))
    }
    if not datasets:
        raise SystemExit(f"没有找到训练文件：{CLEANED_DIR}")
    if args.holdout and args.holdout not in datasets:
        raise SystemExit(f"未知文件：{args.holdout}，可选：{list(datasets)}")

    # 特征只算一次，循环评估时复用
    train_features = {stem: load_features(meta["train"]) for stem, meta in datasets.items()}
    demo_features = {stem: load_features(meta["demo"]) for stem, meta in datasets.items()}
    truth = {stem: pd.read_csv(meta["truth"]) for stem, meta in datasets.items()}

    stems = [args.holdout] if args.holdout else list(datasets)
    print(f"contamination={args.contamination}，特征数={len(FEATURE_COLUMNS)}")

    rows = []
    for stem in stems:
        metrics, cm = evaluate_holdout(
            stem, train_features, demo_features, truth, args.contamination
        )
        rows.append(metrics)
        ratio = metrics["attacks"] / metrics["ips"] if metrics["ips"] else 0
        print(
            f"{stem:10s} IP={metrics['ips']:3d} 攻击={metrics['attacks']:2d}"
            f"({ratio:5.1%}) P={metrics['precision']:.3f} R={metrics['recall']:.3f} "
            f"F1={metrics['f1']:.3f} AUC={metrics['auc']:.3f} "
            f"PR-AUC={metrics['pr_auc']:.3f} 混淆={cm.tolist()}"
        )
        for attack_type, stat in metrics["type_stats"].items():
            hit_rate = stat["detected"] / stat["total"] if stat["total"] else 0
            print(
                f"    类型 {attack_type}：检出 {stat['detected']}/{stat['total']}"
                f" ({hit_rate:.1%})"
            )

    result = pd.DataFrame(rows)
    print(
        "\n平均："
        f"P={result['precision'].mean():.3f} "
        f"R={result['recall'].mean():.3f} "
        f"F1={result['f1'].mean():.3f} "
        f"AUC={result['auc'].mean():.3f} "
        f"PR-AUC={result['pr_auc'].mean():.3f}"
    )

    # 汇总所有文件的攻击类型检出率
    type_totals = {}
    type_detected = {}
    for metrics in rows:
        for attack_type, stat in metrics["type_stats"].items():
            type_totals[attack_type] = type_totals.get(attack_type, 0) + stat["total"]
            type_detected[attack_type] = type_detected.get(attack_type, 0) + stat["detected"]

    if type_totals:
        print("\n分类型召回（合计）：")
        for attack_type in sorted(type_totals):
            total = type_totals[attack_type]
            detected = type_detected[attack_type]
            print(f"  {attack_type:12s} {detected}/{total} ({detected / total:.1%})")

    # 用全部文件的 train 数据训练最终模型
    all_X = pd.concat(train_features.values(), ignore_index=True)
    final_model = make_model(args.contamination)
    final_model.fit(all_X[FEATURE_COLUMNS])

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    bundle = {
        "model": final_model,
        "features": FEATURE_COLUMNS,
        "contamination": args.contamination,
        "random_state": RANDOM_STATE,
        "n_estimators": N_ESTIMATORS,
        "train_stems": sorted(train_features),
        "train_samples": len(all_X),
        "n_features": len(FEATURE_COLUMNS),
        "offset": float(final_model.offset_),
    }
    joblib.dump(bundle, MODEL_PATH)
    print(f"\n最终模型已保存：{MODEL_PATH}")


if __name__ == "__main__":
    main()

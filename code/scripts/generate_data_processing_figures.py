import json
from pathlib import Path
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import font_manager


def configure_chinese_font():
    """自动配置中文字体，避免图表中文乱码。"""
    candidates = [
        "Microsoft YaHei",
        "SimHei",
        "SimSun",
        "Noto Sans CJK SC",
        "WenQuanYi Micro Hei",
        "Arial Unicode MS",
    ]

    available = {f.name for f in font_manager.fontManager.ttflist}
    selected = None
    for font_name in candidates:
        if font_name in available:
            selected = font_name
            break

    if selected:
        plt.rcParams["font.sans-serif"] = [selected] + candidates
    else:
        plt.rcParams["font.sans-serif"] = candidates

    plt.rcParams["axes.unicode_minus"] = False
    print(f"Using font for charts: {selected or 'default fallback'}")

ROOT = Path(r"e:/DATA/jianshen")
CSV_DIR = ROOT / "code" / "csv"
CLEAN_DIR = ROOT / "code" / "data-processing" / "cleaned"
IMG_DIR = ROOT / "output" / "doc" / "images"
OUT_JSON = ROOT / "output" / "doc" / "data_processing_stats.json"

IMG_DIR.mkdir(parents=True, exist_ok=True)
configure_chinese_font()

# raw files
raw_daily_paths = [
    CSV_DIR / "mturkfitbit_export_3.12.16-4.11.16" / "Fitabase Data 3.12.16-4.11.16" / "dailyActivity_merged.csv",
    CSV_DIR / "mturkfitbit_export_4.12.16-5.12.16" / "Fitabase Data 4.12.16-5.12.16" / "dailyActivity_merged.csv",
]
raw_weight_paths = [
    CSV_DIR / "mturkfitbit_export_3.12.16-4.11.16" / "Fitabase Data 3.12.16-4.11.16" / "weightLogInfo_merged.csv",
    CSV_DIR / "mturkfitbit_export_4.12.16-5.12.16" / "Fitabase Data 4.12.16-5.12.16" / "weightLogInfo_merged.csv",
]

raw_daily = pd.concat([pd.read_csv(p) for p in raw_daily_paths], ignore_index=True)
raw_weight = pd.concat([pd.read_csv(p) for p in raw_weight_paths], ignore_index=True)

# dedupe like cleaner
raw_daily_dedup = raw_daily.drop_duplicates()
raw_weight_dedup = raw_weight.drop_duplicates()

# emulate cleaner filters
very_active = pd.to_numeric(raw_daily_dedup.get("VeryActiveMinutes"), errors="coerce").fillna(0)
fairly_active = pd.to_numeric(raw_daily_dedup.get("FairlyActiveMinutes"), errors="coerce").fillna(0)
lightly_active = pd.to_numeric(raw_daily_dedup.get("LightlyActiveMinutes"), errors="coerce").fillna(0)

daily_date = pd.to_datetime(raw_daily_dedup.get("ActivityDate"), errors="coerce")
exercise_emulated = raw_daily_dedup[
    daily_date.notna() & (
        (very_active > 30) |
        (fairly_active > 20) |
        (lightly_active > 30)
    )
].copy()

weight_date = pd.to_datetime(raw_weight_dedup.get("Date"), errors="coerce", format="mixed")
weight_kg = pd.to_numeric(raw_weight_dedup.get("WeightKg"), errors="coerce")
metrics_emulated = raw_weight_dedup[weight_date.notna() & weight_kg.notna()].copy()

# cleaned outputs
clean_ex = pd.read_csv(CLEAN_DIR / "exercise_records.csv")
clean_bm = pd.read_csv(CLEAN_DIR / "body_metrics.csv")

# chart 1: sample-level before/after (same scope)
labels = ["运动数据", "体测数据"]
before_counts = [len(raw_daily_dedup), len(raw_weight_dedup)]
after_counts = [len(exercise_emulated), len(metrics_emulated)]

x = range(len(labels))
plt.figure(figsize=(8, 4.5))
bar_w = 0.35
plt.bar([i - bar_w/2 for i in x], before_counts, width=bar_w, label="清洗前(去重后)")
plt.bar([i + bar_w/2 for i in x], after_counts, width=bar_w, label="清洗后(规则过滤后)")
plt.xticks(list(x), labels)
plt.ylabel("记录数")
plt.title("原始样本清洗前后对比（同口径）")
plt.legend()
plt.tight_layout()
plt.savefig(IMG_DIR / "raw_vs_clean_rowcount.png", dpi=150)
plt.close()

# chart 1b: system-level final scale
system_counts = [len(clean_ex), len(clean_bm)]
plt.figure(figsize=(8, 4.5))
plt.bar(labels, system_counts)
plt.ylabel("记录数")
plt.title("系统清洗后落库规模")
plt.tight_layout()
plt.savefig(IMG_DIR / "cleaned_system_scale.png", dpi=150)
plt.close()

# chart 1c: redesigned comparison figure for thesis
fig, axes = plt.subplots(1, 2, figsize=(12.5, 5.2))

before_color = "#94a3b8"
after_color = "#2563eb"
scale_colors = ["#0f766e", "#f59e0b"]

bars_before = axes[0].bar(
    [i - bar_w / 2 for i in x],
    before_counts,
    width=bar_w,
    label="处理前（去重后）",
    color=before_color,
)
bars_after = axes[0].bar(
    [i + bar_w / 2 for i in x],
    after_counts,
    width=bar_w,
    label="处理后（规则过滤后）",
    color=after_color,
)
axes[0].set_xticks(list(x), labels)
axes[0].set_ylabel("记录数")
axes[0].set_title("(a) 原始样本处理前后对比")
axes[0].legend(fontsize=9, frameon=False)
axes[0].grid(axis="y", linestyle="--", alpha=0.25)
axes[0].set_ylim(0, max(before_counts) * 1.18)

for bar in list(bars_before) + list(bars_after):
    height = int(bar.get_height())
    axes[0].text(
        bar.get_x() + bar.get_width() / 2,
        height + max(before_counts) * 0.02,
        f"{height:,}",
        ha="center",
        va="bottom",
        fontsize=9,
    )

delta_rates = []
for before, after in zip(before_counts, after_counts):
    if before == 0:
        delta_rates.append(0)
    else:
        delta_rates.append((after - before) / before * 100)

for idx, rate in enumerate(delta_rates):
    y_top = max(before_counts[idx], after_counts[idx]) + max(before_counts) * 0.10
    axes[0].annotate(
        f"{rate:.2f}%",
        xy=(idx, y_top),
        ha="center",
        va="bottom",
        fontsize=9,
        color="#b91c1c" if rate < 0 else "#047857",
        fontweight="bold",
    )

bars_scale = axes[1].barh(
    ["运动记录", "体测记录"],
    system_counts,
    color=scale_colors,
    height=0.55,
)
axes[1].set_xlabel("记录数")
axes[1].set_title("(b) 标准化落库后的系统数据规模")
axes[1].grid(axis="x", linestyle="--", alpha=0.25)

for idx, bar in enumerate(bars_scale):
    width = int(bar.get_width())
    user_count = clean_ex["user_id"].nunique() if idx == 0 else clean_bm["user_id"].nunique()
    axes[1].text(
        width + max(system_counts) * 0.01,
        bar.get_y() + bar.get_height() / 2,
        f"{width:,} 条 / {user_count:,} 人",
        va="center",
        fontsize=9,
    )

plt.suptitle("数据处理前后对比与系统落库规模", fontsize=13)
plt.tight_layout()
plt.savefig(IMG_DIR / "cleaning_dual_panel.png", dpi=180)
plt.close()

# chart 2: missing key fields in raw
miss_daily = {
    "ActivityDate": float(raw_daily["ActivityDate"].isna().mean() * 100),
    "TotalSteps": float(raw_daily["TotalSteps"].isna().mean() * 100),
    "Calories": float(raw_daily["Calories"].isna().mean() * 100),
}
miss_weight = {
    "Date": float(raw_weight["Date"].isna().mean() * 100),
    "WeightKg": float(raw_weight["WeightKg"].isna().mean() * 100),
    "BMI": float(raw_weight["BMI"].isna().mean() * 100),
}

heatmap_data = np.array([
    list(miss_daily.values()),
    list(miss_weight.values()),
], dtype=float)
field_labels = [
    ["ActivityDate", "TotalSteps", "Calories"],
    ["Date", "WeightKg", "BMI"],
]
row_labels = ["运动行为样本", "体测样本"]

fig, axes = plt.subplots(2, 1, figsize=(9.8, 5.6))
fig.suptitle("原始样本关键字段缺失率", fontsize=13)

vmax = max(0.1, float(heatmap_data.max()))
cm = plt.cm.Blues

for row_idx, ax in enumerate(axes):
    row = np.array([heatmap_data[row_idx]])
    im = ax.imshow(row, cmap=cm, aspect="auto", vmin=0, vmax=vmax)
    ax.set_yticks([0], [row_labels[row_idx]])
    ax.set_xticks(range(len(field_labels[row_idx])), field_labels[row_idx])
    ax.tick_params(axis="x", rotation=0)
    ax.tick_params(axis="both", length=0)

    for col_idx, value in enumerate(heatmap_data[row_idx]):
        bg = cm(value / vmax if vmax > 0 else 0)
        luminance = 0.299 * bg[0] + 0.587 * bg[1] + 0.114 * bg[2]
        text_color = "#0f172a" if luminance > 0.6 else "white"
        ax.text(
            col_idx,
            0,
            f"{value:.2f}%",
            ha="center",
            va="center",
            fontsize=11,
            fontweight="bold",
            color=text_color,
        )

    for spine in ax.spines.values():
        spine.set_edgecolor("#cbd5e1")
        spine.set_linewidth(1.0)

    ax.set_xticks(np.arange(-0.5, len(field_labels[row_idx]), 1), minor=True)
    ax.set_yticks(np.arange(-0.5, 1, 1), minor=True)
    ax.grid(which="minor", color="white", linestyle="-", linewidth=2)
    ax.tick_params(which="minor", bottom=False, left=False)

fig.text(
    0.5,
    0.02,
    "注：关键字段缺失率均接近 0，说明原始样本在基础字段完整性方面较好。",
    ha="center",
    fontsize=10,
    color="#475569",
)
plt.tight_layout(rect=[0, 0.05, 1, 0.95])
plt.savefig(IMG_DIR / "raw_missing_rate.png", dpi=180)
plt.close()

# chart 3: exercise type distribution in cleaned
top_ex = clean_ex["exercise_type"].astype(str).value_counts().head(10)
plt.figure(figsize=(8, 4.5))
plt.bar(top_ex.index, top_ex.values)
plt.xticks(rotation=30, ha="right")
plt.ylabel("记录数")
plt.title("清洗后运动类型分布(Top10)")
plt.tight_layout()
plt.savefig(IMG_DIR / "clean_exercise_type_top10.png", dpi=150)
plt.close()

# chart 4: bmi distribution in cleaned
if "bmi" in clean_bm.columns:
    bmi = pd.to_numeric(clean_bm["bmi"], errors="coerce").dropna()
    plt.figure(figsize=(8, 4.5))
    plt.hist(bmi, bins=20)
    plt.xlabel("BMI")
    plt.ylabel("人数")
    plt.title("清洗后BMI分布")
    plt.tight_layout()
    plt.savefig(IMG_DIR / "clean_bmi_distribution.png", dpi=150)
    plt.close()

stats = {
    "raw": {
        "daily_rows": int(len(raw_daily)),
        "daily_rows_dedup": int(len(raw_daily_dedup)),
        "weight_rows": int(len(raw_weight)),
        "weight_rows_dedup": int(len(raw_weight_dedup)),
        "daily_unique_users": int(raw_daily_dedup["Id"].nunique()),
        "weight_unique_users": int(raw_weight_dedup["Id"].nunique()),
    },
    "emulated_cleaning": {
        "exercise_rows_after_rule": int(len(exercise_emulated)),
        "metrics_rows_after_rule": int(len(metrics_emulated)),
    },
    "sample_comparison": {
        "exercise_before_dedup": int(len(raw_daily_dedup)),
        "exercise_after_rules": int(len(exercise_emulated)),
        "metrics_before_dedup": int(len(raw_weight_dedup)),
        "metrics_after_rules": int(len(metrics_emulated)),
    },
    "cleaned_output": {
        "exercise_rows": int(len(clean_ex)),
        "body_metrics_rows": int(len(clean_bm)),
        "exercise_unique_users": int(clean_ex["user_id"].nunique()) if "user_id" in clean_ex.columns else None,
        "metrics_unique_users": int(clean_bm["user_id"].nunique()) if "user_id" in clean_bm.columns else None,
    },
    "raw_missing_rate_pct": {
        "daily": miss_daily,
        "weight": miss_weight,
    }
}

OUT_JSON.write_text(json.dumps(stats, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved stats to: {OUT_JSON}")
print(f"Saved charts to: {IMG_DIR}")

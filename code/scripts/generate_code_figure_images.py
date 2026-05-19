from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"e:\DATA\jianshen")
OUTPUT_DIR = ROOT / "output" / "doc" / "images"


FIGURES = [
    {
        "output": "code_fig_5_4a_admin_monitor_metrics.png",
        "title": "AnalyticsService.java  监控指标与活跃率计算",
        "source": ROOT / "code" / "backend" / "src" / "main" / "java" / "com" / "gym" / "fitness" / "service" / "AnalyticsService.java",
        "ranges": [(180, 199)],
    },
    {
        "output": "code_fig_5_4b_admin_behavior_retention.png",
        "title": "AnalyticsService.java  行为趋势与留存率计算",
        "source": ROOT / "code" / "backend" / "src" / "main" / "java" / "com" / "gym" / "fitness" / "service" / "AnalyticsService.java",
        "ranges": [(235, 257)],
    },
    {
        "output": "code_fig_5_5_admin_students.png",
        "title": "AdminService.java  学员管理列表构建核心逻辑",
        "source": ROOT / "code" / "backend" / "src" / "main" / "java" / "com" / "gym" / "fitness" / "service" / "AdminService.java",
        "ranges": [(125, 152)],
    },
    {
        "output": "code_fig_5_6a_coach_dashboard.png",
        "title": "AnalyticsService.java  教练总览统计逻辑",
        "source": ROOT / "code" / "backend" / "src" / "main" / "java" / "com" / "gym" / "fitness" / "service" / "AnalyticsService.java",
        "ranges": [(53, 76)],
    },
    {
        "output": "code_fig_5_6b_coach_student_report.png",
        "title": "AnalyticsService.java  学员报告聚合逻辑",
        "source": ROOT / "code" / "backend" / "src" / "main" / "java" / "com" / "gym" / "fitness" / "service" / "AnalyticsService.java",
        "ranges": [(497, 540)],
    },
]


def load_font(size: int, mono: bool = False):
    candidates = []
    if mono:
        candidates = [
            r"C:\Windows\Fonts\consola.ttf",
            r"C:\Windows\Fonts\cour.ttf",
        ]
    else:
        candidates = [
            r"C:\Windows\Fonts\msyh.ttc",
            r"C:\Windows\Fonts\simsun.ttc",
        ]

    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size=size)
    return ImageFont.load_default()


def read_lines(path: Path, ranges: Iterable[tuple[int, int]]) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    selected = []
    for start, end in ranges:
        for lineno in range(start, min(end, len(lines)) + 1):
            selected.append(f"{lineno:>4}: {lines[lineno - 1]}")
        selected.append("")
    if selected and selected[-1] == "":
        selected.pop()
    return selected


def render_figure(title: str, lines: list[str], output_path: Path):
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    title_font = load_font(30, mono=False)
    code_font = load_font(26, mono=True)

    padding_x = 40
    padding_y = 28
    line_gap = 10
    header_gap = 26
    width = 1900

    temp = Image.new("RGB", (width, 2000), "#f7f8fb")
    draw = ImageDraw.Draw(temp)

    code_line_height = code_font.getbbox("Ag")[3] + line_gap
    title_height = title_font.getbbox(title)[3]
    body_height = code_line_height * len(lines)
    total_height = padding_y * 2 + title_height + header_gap + body_height + 30

    image = Image.new("RGB", (width, total_height), "#f7f8fb")
    draw = ImageDraw.Draw(image)

    draw.rounded_rectangle((18, 18, width - 18, total_height - 18), radius=28, fill="#ffffff", outline="#d7dbe7", width=2)
    draw.text((padding_x, padding_y), title, font=title_font, fill="#1f2a44")

    code_top = padding_y + title_height + header_gap
    draw.rounded_rectangle((padding_x - 12, code_top - 12, width - padding_x + 4, total_height - padding_y), radius=18, fill="#0f172a")

    y = code_top
    for line in lines:
        draw.text((padding_x + 8, y), line, font=code_font, fill="#e5eefc")
        y += code_line_height

    image.save(output_path)


def main():
    for figure in FIGURES:
        lines = read_lines(figure["source"], figure["ranges"])
        render_figure(figure["title"], lines, OUTPUT_DIR / figure["output"])
        print(f"[OK] generated {figure['output']}")


if __name__ == "__main__":
    main()

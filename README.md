# Windows-Style Java Calculator

桌面端标准型计算器，基于 Java 8+ 与 Swing，实现接近 Windows 10/11 Calculator 的交互体验，包含立即执行的四则运算、常用功能键，以及暗色 Fluent 风格界面。

## 功能亮点
- **标准运算**：加、减、乘、除，支持连续输入与立即执行链。
- **一元运算**：百分号、平方根、倒数、取相反数，统一使用 `BigDecimal` 保证精度。
- **状态管理**：CE/C/退格、历史表达式显示、除零等错误提示。
- **键盘映射**：数字、`+ - * /`、Enter、Backspace、Delete、Esc 等快捷键与按钮行为一致。
- **UI 风格**：暗色 Fluent 设计，两行显示屏、圆角按钮、运算符分层配色、悬停/按下亮度过渡。

## 技术栈 & 结构
- **语言**：Java 8+
- **UI**：纯 Swing/AWT（无第三方 GUI 库）
- **模式**：简化 MVC
  - `CalculatorModel` — 状态 / 计算逻辑
  - `CalculatorView` — Swing 窗口与样式
  - `CalculatorController` — 将按钮/键盘事件转为模型操作
  - `CalculatorApp` — 程序入口，装配 MVC

```
src/
└─ calculator/
   ├─ CalculatorModel.java
   ├─ CalculatorView.java
   ├─ CalculatorController.java
   └─ CalculatorApp.java
```

## 构建与运行
```bash
# 1. 编译（输出到 out 目录）
mkdir -p out
javac -d out $(find src -name "*.java")

# 2. 启动
java -cp out calculator.CalculatorApp
```
> 若使用 IDE，可直接导入 `src` 目录并运行 `calculator.CalculatorApp` 主类。

## 常用键盘操作
| 快捷键 | 说明 |
| ------ | ---- |
| `0–9` / 数字小键盘 | 输入数字 |
| `.` / 小键盘 `.` | 小数点 |
| `+ - * /`（含小键盘） | 对应运算符 |
| `Enter` / `=` | 计算等号 |
| `Backspace` | 退格 |
| `Delete` | CE |
| `Esc` | C |

## 后续扩展建议
1. **科学型模式**：增加三角函数、幂、对数，并提供模式切换。
2. **历史记录**：保存多步表达式，允许点击回填。
3. **内存功能**：实现 `MC/MR/M+/M-/MS` 按钮。
4. **主题切换**：在浅色/深色之间动态切换，跟随系统设定。

欢迎 fork/issue/PR 继续完善。***

# myPuzzle 拼图游戏

基于 Java 8+ 与 Swing 的课程项目，实现可加载任意图片的拼图小游戏，遵循 MVC 分层，包含菜单操作、拼图区拖拽交换、难度与挑战模式等功能。

## 功能亮点
- **图片加载**：支持从本地打开任意图片，自动缩放到合适的拼图区尺寸；可预览原图或随机轮换 `resources/images` 目录中的示例图。
- **动态切割**：默认 3×3，可切换 4×4、5×5，切割后保持每块的原始索引以便判断胜利。
- **鼠标拖拽**：在拼图区按下/拖动/释放即可交换两块，立即刷新视图并检测完成状态。
- **挑战模式**：一键开启/停止计时，状态栏展示当前难度与计时；完成拼图后自动提示并停止计时。
- **MVC 解耦**：Model 专注图片数据，View 管理 Swing UI，Controller 监听菜单与鼠标事件，逻辑清晰便于扩展。

## 技术栈 & 结构
- **语言**：Java 8+
- **UI**：Swing（JFrame + JPanel 自绘）
- **模式**：MVC
  - `PuzzleModel` — 图片加载/缩放/切割、块状态与胜利判断
  - `PuzzleView` — 菜单、拼图区、状态栏
  - `PuzzleController` — 菜单逻辑、拖拽交换、挑战计时
  - `Main` — 程序入口，装配 MVC

```
src/
└─ puzzle/
   ├─ PuzzleModel.java
   ├─ PuzzleView.java
   ├─ PuzzleController.java
   └─ Main.java
```

## 构建与运行
```bash
# 1. 编译（需 UTF-8，因为代码含中文注释）
javac -encoding UTF-8 src/puzzle/*.java

# 2. 启动
java -cp src/puzzle Main
```
> 也可以使用 IDE 直接运行 `puzzle.Main` 主类；若希望随机切换示例图片，可在项目根目录创建 `resources/images` 并放入若干 png/jpg。

## 使用提示
- 通过“图片”菜单打开新图、查看原图、随机切换；
- “难度设置”菜单可即时切换 3×3 / 4×4 / 5×5；
- “挑战模式”可开始或停止计时，底部状态栏实时显示。

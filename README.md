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
# 1. 编译（需 UTF-8，因为代码含中文注释）
mkdir out
javac -encoding UTF-8 -d out *.java 

# 2. 启动
java -cp out CalculatorApp
```

> 若使用 IDE，可直接导入 `src` 目录并运行 `calculator.CalculatorApp` 主类。

## 常用键盘操作

| 快捷键                | 说明       |
| --------------------- | ---------- |
| `0–9` / 数字小键盘    | 输入数字   |
| `.` / 小键盘 `.`      | 小数点     |
| `+ - * /`（含小键盘） | 对应运算符 |
| `Enter` / `=`         | 计算等号   |
| `Backspace`           | 退格       |
| `Delete`              | CE         |
| `Esc`                 | C          |



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
mkdir out
javac -encoding UTF-8 -d out *.java

# 2. 启动
java -cp out Main
```

> 也可以使用 IDE 直接运行 `puzzle.Main` 主类；若希望随机切换示例图片，可在项目根目录创建 `resources/images` 并放入若干 png/jpg。

## 使用提示

- 通过“图片”菜单打开新图、查看原图、随机切换；
- “难度设置”菜单可即时切换 3×3 / 4×4 / 5×5；
- “挑战模式”可开始或停止计时，底部状态栏实时显示。

# SimpleMDIExample 多文档文本编辑器

基于 Java 8+ Swing 与 MVC 架构的课程项目，实现类似 WordPad 的多文档文本编辑器，支持新建/打开/保存/字体样式以及 MDI 窗口管理。

## 功能亮点

- **MDI 管理**：使用 `JDesktopPane + JInternalFrame`，可创建多个文档窗口，支持层叠、水平/垂直平铺。
- **文件操作**：新建、打开 `.txt`、保存、另存为；关闭窗口前检测未保存内容。
- **格式控制**：字体对话框、粗体/斜体/下划线切换，工具栏与菜单双入口。
- **状态同步**：每个文档维护独立 `DocumentModel`，含内容、路径、字体、dirty 标志；窗口标题用 `*` 标记未保存。
- **扩展预留**：控制器已预留结构，后续可添加撤销/重做、剪贴板、查找替换等功能。

## 技术栈 & 结构

- **语言**：Java 8+
- **UI**：Swing
- **模式**：MVC
  - `DocumentModel` — 文档内容、字体与保存状态
  - `MainFrame` — 主窗体、菜单、工具栏、MDI 管理
  - `DocumentController` — 文件读写、字体/窗口操作、dirty 检查
  - `Main` — 应用入口

```
src/
└─ simpleMDIExample/
   ├─ DocumentModel.java
   ├─ DocumentController.java
   ├─ MainFrame.java
   └─ Main.java
```

## 构建与运行

```bash
# 1. 编译（需 UTF-8，因为代码含中文注释）
mkdir out
javac -encoding UTF-8 -d out *.java

# 2. 启动
java -cp out Main
```

> 新建文档默认铺满桌面区域；通过菜单“文件/格式/窗口”即可体验全部核心功能。
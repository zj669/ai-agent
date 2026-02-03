---
inclusion: always
---

# 命令使用限制规范

## 禁止使用的命令

### 文件读取命令（编码问题）

**严格禁止使用以下 PowerShell 命令读取包含中文的文件：**

- `Get-Content` - 在处理 UTF-8 中文时会产生乱码
- `cat` - 同样存在编码问题
- `type` - CMD 命令，编码处理不可靠

**原因：** 这些命令在 Windows 环境下处理 UTF-8 编码的中文文件时，经常出现乱码问题，导致文件内容无法正确显示。

### 正确的替代方案

对于工作区内的文件：
- 使用 `readFile` 工具
- 使用 `readMultipleFiles` 工具
- 使用 `grepSearch` 工具搜索内容

对于工作区外的文件：
- 使用 `.kiro/scripts/file_operations.py` Python 脚本
- 该脚本可靠地处理 UTF-8 编码，支持中文

## 文件写入命令限制

**禁止使用以下命令写入文件：**

- `Set-Content` - 编码问题
- `Out-File` - 编码问题
- `>` 或 `>>` 重定向 - 编码不可控

**正确做法：**
- 工作区内文件：使用 `fsWrite` 或 `fsAppend` 工具
- 工作区外文件：使用 `.kiro/scripts/file_operations.py` 脚本

## Python 脚本使用方法

对于工作区外的文件操作，使用 `.kiro/scripts/file_operations.py`：

### 读取文件
```powershell
python .kiro/scripts/file_operations.py read "C:/Users/xxx/.kiro/steering/chinese-language.md"
```

### 写入文件（覆盖）
```powershell
python .kiro/scripts/file_operations.py write "C:/Users/xxx/.kiro/steering/test.md" "中文内容
可以多行"
```

### 追加文件
```powershell
python .kiro/scripts/file_operations.py append "C:/Users/xxx/.kiro/steering/test.md" "追加的中文内容"
```

**注意：** 
- 文件路径支持 `~` 表示用户目录
- 内容参数支持多行文本
- 自动使用 UTF-8 编码，完美支持中文

## 示例

❌ **错误做法：**
```powershell
Get-Content "C:/Users/xxx/.kiro/steering/chinese-language.md" -Encoding UTF8
Set-Content -Path "file.txt" -Value "中文内容"
```

✅ **正确做法：**
```
# 工作区内文件
使用 readFile 工具读取
使用 fsWrite 工具写入

# 工作区外文件
python .kiro/scripts/file_operations.py read "C:/Users/xxx/.kiro/steering/chinese-language.md"
python .kiro/scripts/file_operations.py write "C:/Users/xxx/.kiro/steering/test.md" "中文内容"
```

## 强制规则

当需要操作包含中文内容的文件时：
1. 首先检查文件是否在工作区内
2. 工作区内：必须使用文件工具（readFile, fsWrite 等）
3. 工作区外：必须使用 `.kiro/scripts/file_operations.py` Python 脚本
4. **绝对不要使用 PowerShell 文本命令处理中文文件**

## 脚本优势

使用 Python 脚本的好处：
- ✅ 完美支持 UTF-8 编码
- ✅ 可靠处理中文字符
- ✅ 跨平台兼容
- ✅ 自动创建父目录
- ✅ 清晰的错误提示

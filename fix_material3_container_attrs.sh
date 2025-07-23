#!/bin/bash

# 修复Material 3 Container颜色属性兼容性问题
echo "修复Material 3 Container颜色属性..."

# 进入资源目录
cd "/Users/a1/AndroidStudioProjects/OmniControl/app/src/main/res"

# 替换Material 3的Container颜色属性为AppCompat兼容属性
echo "替换 ?attr/colorSecondaryContainer → @color/light_gray"
find . -name "*.xml" -exec sed -i '' 's/?attr\/colorSecondaryContainer/@color\/light_gray/g' {} \;

echo "替换 ?attr/colorOnSecondaryContainer → ?android:attr/textColorSecondary"
find . -name "*.xml" -exec sed -i '' 's/?attr\/colorOnSecondaryContainer/?android:attr\/textColorSecondary/g' {} \;

echo "Material 3 Container属性修复完成！"

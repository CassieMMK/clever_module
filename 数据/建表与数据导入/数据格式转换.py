import os
import pandas as pd

# 设置源文件夹路径和目标文件夹路径
source_folder = 'C:\\Users\\kanpeiming\\Desktop\\项目\\建表与数据导入'
target_folder = 'C:\\Users\\kanpeiming\\Desktop\\项目\\数据csv格式'

# 确保目标文件夹存在
if not os.path.exists(target_folder):
    os.makedirs(target_folder)

# 遍历源文件夹中的所有.xlsx文件
for file in os.listdir(source_folder):
    if file.endswith('.xlsx'):
        # 构建完整的文件路径
        file_path = os.path.join(source_folder, file)
        # 读取.xlsx文件
        excel_data = pd.read_excel(file_path, engine='openpyxl')
        # 构建CSV文件名
        csv_file = os.path.splitext(file)[0] + '.csv'
        # 构建完整的CSV文件路径
        csv_path = os.path.join(target_folder, csv_file)
        # 将数据写入CSV文件
        excel_data.to_csv(csv_path, index=False)
        print(f'转换完成：{file} -> {csv_file}')

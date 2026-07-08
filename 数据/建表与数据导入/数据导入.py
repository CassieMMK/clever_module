import pandas as pd
import mysql.connector

# 步骤1: 读取Excel文件
excel_file = 'C:\\Users\\kanpeiming\\Desktop\\项目\\建表与数据导入\\national_social_science_aware_info.xlsx'  # 替换为你的Excel文件路径
df = pd.read_excel(excel_file)

# 步骤2: 连接数据库
db_connection = mysql.connector.connect(
    host="127.0.0.1",  # 数据库主机地址
    user="root",  # 数据库用户名
    passwd="takaki",  # 数据库密码
    database="skldata"  # 数据库名
)

# 步骤3: 导入数据
cursor = db_connection.cursor()

# 创建插入数据的SQL语句
table_name = 'national_social_science_aware_info'  # 替换为你的数据表名
columns = ', '.join(df.columns)
placeholders = ', '.join(['%s'] * len(df.columns))
insert_query = f"INSERT INTO {table_name} ({columns}) VALUES ({placeholders})"

# 将DataFrame中的每一行数据插入到数据库中
for index, row in df.iterrows():
    cursor.execute(insert_query, tuple(row))

# 提交事务
db_connection.commit()

# 关闭游标和连接
cursor.close()
db_connection.close()

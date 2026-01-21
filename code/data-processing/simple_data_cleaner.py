"""
简化的数据清洗脚本
使用pandas代替Spark，避免Windows上的Hadoop依赖问题
"""
import pandas as pd
import json
import os
from datetime import datetime

def clean_user_data(input_path, output_path):
    """清洗用户数据"""
    print(f"开始清洗用户数据: {input_path}")
    
    # 读取JSON数据
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    df = pd.DataFrame(data)
    
    # 数据清洗
    # 1. 去重
    df = df.drop_duplicates(subset=['user_id'])
    
    # 2. 过滤空值
    df = df[df['user_id'].notna()]
    df = df[df['username'].notna()]
    
    # 3. 格式统一
    df['username'] = df['username'].str.lower().str.strip()
    df['email'] = df['email'].str.lower().str.strip()
    df['phone'] = df['phone'].str.replace(r'[^\d]', '', regex=True)
    
    # 4. 数据验证
    df = df[(df['age'] >= 15) & (df['age'] <= 80)]
    df = df[df['gender'].isin(['男', '女'])]
    
    # 保存清洗后的数据
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    df.to_csv(output_path, index=False, encoding='utf-8')
    
    print(f"用户数据清洗完成，共 {len(df)} 条记录")
    return df

def clean_exercise_records(input_path, output_path):
    """清洗运动记录数据"""
    print(f"开始清洗运动记录数据: {input_path}")
    
    # 读取JSON数据
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    df = pd.DataFrame(data)
    
    # 数据清洗
    # 1. 去重
    df = df.drop_duplicates(subset=['record_id'])
    
    # 2. 过滤空值
    df = df[df['record_id'].notna()]
    df = df[df['user_id'].notna()]
    df = df[df['exercise_type'].notna()]
    
    # 3. 日期转换
    df['exercise_date'] = pd.to_datetime(df['exercise_date'])
    
    # 4. 数据验证
    df = df[(df['duration_minutes'] >= 5) & (df['duration_minutes'] <= 300)]
    df = df[df['calories_burned'] > 0]
    df = df[(df['average_heart_rate'] >= 60) & (df['average_heart_rate'] <= 200)]
    df = df[(df['max_heart_rate'] >= 80) & (df['max_heart_rate'] <= 220)]
    
    # 5. 格式统一
    df['exercise_type'] = df['exercise_type'].str.strip()
    df['equipment_used'] = df['equipment_used'].str.strip()
    
    # 保存清洗后的数据
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    df.to_csv(output_path, index=False, encoding='utf-8')
    
    print(f"运动记录数据清洗完成，共 {len(df)} 条记录")
    return df

def clean_body_metrics(input_path, output_path):
    """清洗身体指标数据"""
    print(f"开始清洗身体指标数据: {input_path}")
    
    # 读取JSON数据
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    df = pd.DataFrame(data)
    
    # 数据清洗
    # 1. 去重
    df = df.drop_duplicates(subset=['metric_id'])
    
    # 2. 过滤空值
    df = df[df['metric_id'].notna()]
    df = df[df['user_id'].notna()]
    df = df[df['measurement_date'].notna()]
    df = df[df['weight_kg'].notna()]
    
    # 3. 日期转换
    df['measurement_date'] = pd.to_datetime(df['measurement_date'])
    
    # 4. 数据验证
    df = df[(df['weight_kg'] >= 30) & (df['weight_kg'] <= 200)]
    df = df[(df['body_fat_percentage'] >= 5) & (df['body_fat_percentage'] <= 50)]
    df = df[(df['height_cm'] >= 140) & (df['height_cm'] <= 220)]
    df = df[(df['bmi'] >= 10) & (df['bmi'] <= 50)]
    
    # 5. 重新计算BMI确保准确性
    df['bmi'] = df['weight_kg'] / ((df['height_cm'] / 100) ** 2)
    
    # 保存清洗后的数据
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    df.to_csv(output_path, index=False, encoding='utf-8')
    
    print(f"身体指标数据清洗完成，共 {len(df)} 条记录")
    return df

def main():
    print("="*50)
    print("健身数据清洗")
    print("="*50)
    
    # 输入输出路径
    input_base = 'data-collection/output'
    output_base = 'data-processing/cleaned'
    
    # 清洗用户数据
    users_df = clean_user_data(
        f'{input_base}/users.json',
        f'{output_base}/users.csv'
    )
    
    # 清洗运动记录
    exercise_df = clean_exercise_records(
        f'{input_base}/exercise_records.json',
        f'{output_base}/exercise_records.csv'
    )
    
    # 清洗身体指标
    metrics_df = clean_body_metrics(
        f'{input_base}/body_metrics.json',
        f'{output_base}/body_metrics.csv'
    )
    
    print("\n" + "="*50)
    print("数据清洗完成！")
    print("="*50)
    print(f"用户数: {len(users_df)}")
    print(f"运动记录数: {len(exercise_df)}")
    print(f"身体指标数: {len(metrics_df)}")
    print(f"\n清洗后的数据保存在: {output_base}/")

if __name__ == '__main__':
    main()

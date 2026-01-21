"""
Spark数据清洗服务
使用PySpark进行数据清洗、去重、格式统一和缺失值处理
"""
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, when, trim, lower, to_date, regexp_replace
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, DoubleType, DateType
import os

class SparkDataCleaner:
    def __init__(self, app_name="GymFitnessDataCleaner"):
        """初始化Spark Session"""
        self.spark = SparkSession.builder \
            .appName(app_name) \
            .config("spark.sql.adaptive.enabled", "true") \
            .getOrCreate()
        
        self.spark.sparkContext.setLogLevel("WARN")
    
    def clean_user_data(self, input_path, output_path):
        """清洗用户数据"""
        print(f"开始清洗用户数据: {input_path}")
        
        # 读取数据 - 使用multiLine选项读取JSON数组
        df = self.spark.read.option("multiLine", "true").json(input_path)
        
        # 数据清洗
        df_cleaned = df \
            .dropDuplicates(['user_id']) \
            .filter(col('user_id').isNotNull()) \
            .filter(col('username').isNotNull()) \
            .withColumn('username', trim(lower(col('username')))) \
            .withColumn('email', trim(lower(col('email')))) \
            .withColumn('phone', regexp_replace(col('phone'), r'[^\d]', '')) \
            .filter(col('age').between(15, 80)) \
            .filter(col('gender').isin(['男', '女']))
        
        # 保存清洗后的数据 - 使用CSV格式避免Windows上的Hadoop问题
        df_cleaned.write.mode('overwrite').option("header", "true").csv(output_path)
        
        count = df_cleaned.count()
        print(f"用户数据清洗完成，共 {count} 条记录")
        return df_cleaned
    
    def clean_exercise_records(self, input_path, output_path):
        """清洗运动记录数据"""
        print(f"开始清洗运动记录数据: {input_path}")
        
        # 读取数据 - 使用multiLine选项读取JSON数组
        df = self.spark.read.option("multiLine", "true").json(input_path)
        
        # 数据清洗
        df_cleaned = df \
            .dropDuplicates(['record_id']) \
            .filter(col('record_id').isNotNull()) \
            .filter(col('user_id').isNotNull()) \
            .filter(col('exercise_type').isNotNull()) \
            .withColumn('exercise_date', to_date(col('exercise_date'))) \
            .filter(col('duration_minutes').between(5, 300)) \
            .filter(col('calories_burned') > 0) \
            .filter(col('average_heart_rate').between(60, 200)) \
            .filter(col('max_heart_rate').between(80, 220)) \
            .withColumn('exercise_type', trim(col('exercise_type'))) \
            .withColumn('equipment_used', trim(col('equipment_used'))) \
            .na.drop(subset=['exercise_date', 'duration_minutes'])
        
        # 保存清洗后的数据 - 使用CSV格式避免Windows上的Hadoop问题
        df_cleaned.write.mode('overwrite').partitionBy('exercise_date').option("header", "true").csv(output_path)
        
        count = df_cleaned.count()
        print(f"运动记录数据清洗完成，共 {count} 条记录")
        return df_cleaned
    
    def clean_body_metrics(self, input_path, output_path):
        """清洗身体指标数据"""
        print(f"开始清洗身体指标数据: {input_path}")
        
        # 读取数据 - 使用multiLine选项读取JSON数组
        df = self.spark.read.option("multiLine", "true").json(input_path)
        
        # 数据清洗
        df_cleaned = df \
            .dropDuplicates(['metric_id']) \
            .filter(col('metric_id').isNotNull()) \
            .filter(col('user_id').isNotNull()) \
            .withColumn('measurement_date', to_date(col('measurement_date'))) \
            .filter(col('weight_kg').between(30, 200)) \
            .filter(col('body_fat_percentage').between(5, 50)) \
            .filter(col('height_cm').between(140, 220)) \
            .filter(col('bmi').between(10, 50)) \
            .na.drop(subset=['measurement_date', 'weight_kg'])
        
        # 重新计算BMI确保准确性
        df_cleaned = df_cleaned.withColumn(
            'bmi',
            col('weight_kg') / ((col('height_cm') / 100) * (col('height_cm') / 100))
        )
        
        # 保存清洗后的数据 - 使用CSV格式避免Windows上的Hadoop问题
        df_cleaned.write.mode('overwrite').partitionBy('measurement_date').option("header", "true").csv(output_path)
        
        count = df_cleaned.count()
        print(f"身体指标数据清洗完成，共 {count} 条记录")
        return df_cleaned
    
    def remove_duplicates_advanced(self, df, key_columns):
        """高级去重：保留最新的记录"""
        from pyspark.sql.window import Window
        from pyspark.sql.functions import row_number, desc
        
        window_spec = Window.partitionBy(key_columns).orderBy(desc('created_at'))
        
        df_deduped = df \
            .withColumn('row_num', row_number().over(window_spec)) \
            .filter(col('row_num') == 1) \
            .drop('row_num')
        
        return df_deduped
    
    def validate_data_quality(self, df, data_type):
        """验证数据质量"""
        print(f"\n{data_type} 数据质量报告:")
        print(f"总记录数: {df.count()}")
        print(f"列数: {len(df.columns)}")
        
        # 检查空值
        print("\n空值统计:")
        for column in df.columns:
            null_count = df.filter(col(column).isNull()).count()
            if null_count > 0:
                print(f"  {column}: {null_count}")
        
        # 显示样本数据
        print("\n样本数据:")
        df.show(5, truncate=False)
    
    def stop(self):
        """停止Spark Session"""
        self.spark.stop()

def main():
    """主函数"""
    cleaner = SparkDataCleaner()
    
    # 定义路径
    input_base = 'code/data-collection/output'
    output_base = 'code/data-processing/cleaned'
    
    # 创建输出目录
    os.makedirs(output_base, exist_ok=True)
    
    try:
        # 清洗用户数据
        users_df = cleaner.clean_user_data(
            f'{input_base}/users.json',
            f'{output_base}/users'
        )
        cleaner.validate_data_quality(users_df, '用户')
        
        # 清洗运动记录
        exercise_df = cleaner.clean_exercise_records(
            f'{input_base}/exercise_records.json',
            f'{output_base}/exercise_records'
        )
        cleaner.validate_data_quality(exercise_df, '运动记录')
        
        # 清洗身体指标
        metrics_df = cleaner.clean_body_metrics(
            f'{input_base}/body_metrics.json',
            f'{output_base}/body_metrics'
        )
        cleaner.validate_data_quality(metrics_df, '身体指标')
        
        print("\n所有数据清洗完成！")
        
    finally:
        cleaner.stop()

if __name__ == '__main__':
    main()

"""
Spark数据分析组件
分析用户行为、健身效果、运动关联等
"""
from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, count, sum as spark_sum, avg, max as spark_max, min as spark_min,
    hour, dayofweek, date_format, row_number, rank, dense_rank,
    first, last, lag, lead, when, lit, round as spark_round
)
from pyspark.sql.window import Window
import mysql.connector
from datetime import datetime, timedelta
import json

class SparkAnalyzer:
    def __init__(self, mysql_config):
        """初始化Spark分析器"""
        self.spark = SparkSession.builder \
            .appName("GymFitnessAnalyzer") \
            .config("spark.sql.adaptive.enabled", "true") \
            .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
            .getOrCreate()
        
        self.mysql_config = mysql_config
        print("Spark分析器初始化完成")
    
    def load_data_from_csv(self, users_path, exercise_path, metrics_path):
        """从CSV加载数据"""
        print("\n加载数据...")
        
        self.users_df = self.spark.read.csv(users_path, header=True, inferSchema=True)
        self.exercise_df = self.spark.read.csv(exercise_path, header=True, inferSchema=True)
        self.metrics_df = self.spark.read.csv(metrics_path, header=True, inferSchema=True)
        
        print(f"用户数: {self.users_df.count()}")
        print(f"运动记录数: {self.exercise_df.count()}")
        print(f"身体指标数: {self.metrics_df.count()}")
    
    def analyze_user_behavior(self):
        """用户行为分析"""
        print("\n=== 用户行为分析 ===")
        
        # 1. 最受欢迎的运动类型
        popular_exercises = self.exercise_df.groupBy("exercise_type") \
            .agg(
                count("*").alias("count"),
                spark_sum("duration_minutes").alias("total_duration")
            ) \
            .orderBy(col("count").desc())
        
        print("\n最受欢迎的运动类型:")
        popular_exercises.show(10)
        
        most_popular = popular_exercises.first()
        
        # 2. 活跃时段分析（假设我们有时间数据，这里用日期的小时部分模拟）
        # 实际应该从exercise_records的时间戳中提取
        
        # 3. 平均运动时长
        avg_duration = self.exercise_df.agg(
            avg("duration_minutes").alias("avg_duration")
        ).first()["avg_duration"]
        
        print(f"\n平均运动时长: {avg_duration:.2f} 分钟")
        
        # 4. 活跃用户数（最近30天有运动记录的用户）
        from pyspark.sql.functions import to_date, current_date, datediff
        
        self.exercise_df = self.exercise_df.withColumn(
            "exercise_date", to_date(col("exercise_date"))
        )
        
        active_users = self.exercise_df.filter(
            datediff(current_date(), col("exercise_date")) <= 30
        ).select("user_id").distinct().count()
        
        print(f"活跃用户数（最近30天）: {active_users}")
        
        # 5. 运动类型分布（JSON格式）
        exercise_distribution = popular_exercises.collect()
        distribution_dict = {
            row["exercise_type"]: {
                "count": row["count"],
                "total_duration": float(row["total_duration"])
            }
            for row in exercise_distribution
        }
        
        # 返回分析结果
        return {
            "analysis_date": datetime.now().date().isoformat(),
            "most_popular_exercise": most_popular["exercise_type"],
            "peak_hour_start": 18,  # 模拟数据
            "peak_hour_end": 20,
            "average_duration_minutes": round(avg_duration, 2),
            "active_user_count": active_users,
            "exercise_type_distribution": json.dumps(distribution_dict, ensure_ascii=False)
        }
    
    def analyze_fitness_effect(self):
        """健身效果分析"""
        print("\n=== 健身效果分析 ===")
        
        # 1. 体重变化趋势
        window_spec = Window.partitionBy("user_id").orderBy("measurement_date")
        
        metrics_with_change = self.metrics_df.withColumn(
            "prev_weight", lag("weight_kg").over(window_spec)
        ).withColumn(
            "weight_change", 
            when(col("prev_weight").isNotNull(), 
                 col("prev_weight") - col("weight_kg")).otherwise(0)
        )
        
        # 2. 总体减重统计
        weight_loss_stats = metrics_with_change.filter(col("weight_change") > 0) \
            .agg(
                count("*").alias("records_with_loss"),
                spark_sum("weight_change").alias("total_weight_loss"),
                avg("weight_change").alias("avg_weight_loss")
            )
        
        print("\n减重统计:")
        weight_loss_stats.show()
        
        # 3. 用户减重排名
        user_weight_change = self.metrics_df.groupBy("user_id").agg(
            first("weight_kg").alias("initial_weight"),
            last("weight_kg").alias("current_weight")
        ).withColumn(
            "weight_loss", col("initial_weight") - col("current_weight")
        ).filter(col("weight_loss") > 0)
        
        weight_loss_ranking = user_weight_change.withColumn(
            "rank", rank().over(Window.orderBy(col("weight_loss").desc()))
        ).orderBy("rank")
        
        print("\n减重排行榜（Top 10）:")
        weight_loss_ranking.show(10)
        
        return weight_loss_ranking
    
    def analyze_exercise_association(self):
        """运动关联分析"""
        print("\n=== 运动关联分析 ===")
        
        # 分析用户经常一起进行的运动类型
        # 使用窗口函数找出同一用户在相近日期的运动组合
        
        window_spec = Window.partitionBy("user_id").orderBy("exercise_date")
        
        exercise_with_next = self.exercise_df.withColumn(
            "next_exercise", lead("exercise_type").over(window_spec)
        ).withColumn(
            "next_date", lead("exercise_date").over(window_spec)
        )
        
        # 找出在7天内进行的运动组合
        from pyspark.sql.functions import datediff
        
        exercise_pairs = exercise_with_next.filter(
            (col("next_exercise").isNotNull()) &
            (datediff(col("next_date"), col("exercise_date")) <= 7)
        ).groupBy("exercise_type", "next_exercise") \
            .agg(count("*").alias("pair_count")) \
            .orderBy(col("pair_count").desc())
        
        print("\n运动组合关联（7天内）:")
        exercise_pairs.show(20)
        
        return exercise_pairs
    
    def generate_leaderboard(self):
        """生成排行榜数据"""
        print("\n=== 生成排行榜 ===")
        
        # 1. 总运动时长排行榜
        duration_leaderboard = self.exercise_df.groupBy("user_id") \
            .agg(spark_sum("duration_minutes").alias("total_duration")) \
            .withColumn("rank", rank().over(Window.orderBy(col("total_duration").desc()))) \
            .orderBy("rank") \
            .limit(100)
        
        print("\n总运动时长排行榜（Top 10）:")
        duration_leaderboard.show(10)
        
        # 2. 总消耗卡路里排行榜
        calorie_leaderboard = self.exercise_df.groupBy("user_id") \
            .agg(spark_sum("calories_burned").alias("total_calories")) \
            .withColumn("rank", rank().over(Window.orderBy(col("total_calories").desc()))) \
            .orderBy("rank") \
            .limit(100)
        
        print("\n总消耗卡路里排行榜（Top 10）:")
        calorie_leaderboard.show(10)
        
        # 3. 减重排行榜
        weight_loss_leaderboard = self.analyze_fitness_effect()
        
        return {
            "duration": duration_leaderboard,
            "calories": calorie_leaderboard,
            "weight_loss": weight_loss_leaderboard
        }
    
    def save_to_mysql(self, table_name, data_dict):
        """保存分析结果到MySQL"""
        print(f"\n保存分析结果到MySQL表: {table_name}")
        
        try:
            connection = mysql.connector.connect(**self.mysql_config)
            cursor = connection.cursor()
            
            if table_name == "user_behavior_analysis":
                insert_query = """
                INSERT INTO user_behavior_analysis 
                (analysis_date, most_popular_exercise, peak_hour_start, peak_hour_end,
                 average_duration_minutes, active_user_count, exercise_type_distribution)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    most_popular_exercise = VALUES(most_popular_exercise),
                    peak_hour_start = VALUES(peak_hour_start),
                    peak_hour_end = VALUES(peak_hour_end),
                    average_duration_minutes = VALUES(average_duration_minutes),
                    active_user_count = VALUES(active_user_count),
                    exercise_type_distribution = VALUES(exercise_type_distribution)
                """
                
                values = (
                    data_dict["analysis_date"],
                    data_dict["most_popular_exercise"],
                    data_dict["peak_hour_start"],
                    data_dict["peak_hour_end"],
                    data_dict["average_duration_minutes"],
                    data_dict["active_user_count"],
                    data_dict["exercise_type_distribution"]
                )
                
                cursor.execute(insert_query, values)
                connection.commit()
                print(f"成功保存用户行为分析结果")
            
            cursor.close()
            connection.close()
            
        except Exception as e:
            print(f"保存到MySQL时出错: {e}")
    
    def save_leaderboard_to_mysql(self, leaderboards, period_start, period_end):
        """保存排行榜到MySQL"""
        print(f"\n保存排行榜到MySQL (期间: {period_start} 到 {period_end})")
        
        try:
            connection = mysql.connector.connect(**self.mysql_config)
            cursor = connection.cursor()
            
            # 清空当前周期的排行榜
            delete_query = """
            DELETE FROM leaderboards 
            WHERE period_start = %s AND period_end = %s
            """
            cursor.execute(delete_query, (period_start, period_end))
            
            # 插入总运动时长排行榜
            duration_data = leaderboards["duration"].collect()
            for row in duration_data:
                insert_query = """
                INSERT INTO leaderboards 
                (leaderboard_type, user_id, rank, value, period_start, period_end)
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                values = (
                    'TOTAL_DURATION',
                    int(row["user_id"]),
                    int(row["rank"]),
                    float(row["total_duration"]),
                    period_start,
                    period_end
                )
                cursor.execute(insert_query, values)
            
            # 插入总消耗卡路里排行榜
            calorie_data = leaderboards["calories"].collect()
            for row in calorie_data:
                insert_query = """
                INSERT INTO leaderboards 
                (leaderboard_type, user_id, rank, value, period_start, period_end)
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                values = (
                    'TOTAL_CALORIES',
                    int(row["user_id"]),
                    int(row["rank"]),
                    float(row["total_calories"]),
                    period_start,
                    period_end
                )
                cursor.execute(insert_query, values)
            
            # 插入减重排行榜
            weight_loss_data = leaderboards["weight_loss"].collect()
            for row in weight_loss_data:
                insert_query = """
                INSERT INTO leaderboards 
                (leaderboard_type, user_id, rank, value, period_start, period_end)
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                values = (
                    'WEIGHT_LOSS',
                    int(row["user_id"]),
                    int(row["rank"]),
                    float(row["weight_loss"]),
                    period_start,
                    period_end
                )
                cursor.execute(insert_query, values)
            
            connection.commit()
            print(f"成功保存排行榜数据")
            
            cursor.close()
            connection.close()
            
        except Exception as e:
            print(f"保存排行榜到MySQL时出错: {e}")
    
    def run_full_analysis(self):
        """运行完整分析"""
        print("\n" + "="*60)
        print("开始完整数据分析")
        print("="*60)
        
        # 1. 用户行为分析
        behavior_result = self.analyze_user_behavior()
        self.save_to_mysql("user_behavior_analysis", behavior_result)
        
        # 2. 健身效果分析
        self.analyze_fitness_effect()
        
        # 3. 运动关联分析
        self.analyze_exercise_association()
        
        # 4. 生成排行榜
        leaderboards = self.generate_leaderboard()
        
        # 保存排行榜（最近30天）
        period_end = datetime.now().date()
        period_start = period_end - timedelta(days=30)
        self.save_leaderboard_to_mysql(leaderboards, period_start, period_end)
        
        print("\n" + "="*60)
        print("数据分析完成！")
        print("="*60)
    
    def stop(self):
        """停止Spark会话"""
        self.spark.stop()
        print("\nSpark会话已关闭")

def main():
    # MySQL配置
    mysql_config = {
        'host': 'localhost',
        'user': 'root',
        'password': 'your_password',  # 请修改为实际密码
        'database': 'gym_fitness_analytics',
        'charset': 'utf8mb4'
    }
    
    # 创建分析器
    analyzer = SparkAnalyzer(mysql_config)
    
    try:
        # 加载数据
        analyzer.load_data_from_csv(
            'data-processing/cleaned/users.csv',
            'data-processing/cleaned/exercise_records.csv',
            'data-processing/cleaned/body_metrics.csv'
        )
        
        # 运行完整分析
        analyzer.run_full_analysis()
        
    finally:
        # 关闭Spark会话
        analyzer.stop()

if __name__ == '__main__':
    main()

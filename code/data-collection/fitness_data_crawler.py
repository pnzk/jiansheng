"""
健身数据多源采集器
支持多种数据获取方式：
1. Kaggle数据集下载
2. 公开API数据获取
3. 高质量模拟数据生成
4. CSV/JSON文件处理
"""
import os
import sys
import json
import random
import requests
import subprocess
from datetime import datetime, timedelta
from typing import List, Dict, Optional
import pandas as pd
import numpy as np

# 设置随机种子
random.seed(42)
np.random.seed(42)

class FitnessDataCrawler:
    """健身数据采集器"""
    
    # Kaggle健身相关数据集
    KAGGLE_DATASETS = [
        {'name': 'aroojanwarkhan/fitness-data-trends', 'desc': '健身趋势数据'},
        {'name': 'kukuroo3/body-performance-data', 'desc': '身体表现数据'},
        {'name': 'valakhorasani/gym-members-exercise-dataset', 'desc': '健身房会员数据'},
        {'name': 'niharika41298/gym-exercise-data', 'desc': '健身运动数据'},
        {'name': 'aakashjoshi123/exercise-and-fitness-metrics-dataset', 'desc': '运动健身指标'},
        {'name': 'rishikeshkonapure/fitness-trackers-products-ecommerce', 'desc': '健身追踪器数据'},
        {'name': 'fmendes/fmendesdat263xdemos', 'desc': '健身演示数据'},
        {'name': 'nithilaa/fitness-analysis', 'desc': '健身分析数据'},
    ]
    
    # 公开健身API列表
    PUBLIC_APIS = [
        {
            'name': 'ExerciseDB',
            'url': 'https://exercisedb.p.rapidapi.com/exercises',
            'desc': '运动数据库API',
            'requires_key': True
        },
        {
            'name': 'Wger',
            'url': 'https://wger.de/api/v2/exercise/',
            'desc': '开源健身API',
            'requires_key': False
        },
        {
            'name': 'API Ninjas',
            'url': 'https://api.api-ninjas.com/v1/exercises',
            'desc': '运动API',
            'requires_key': True
        }
    ]
    
    def __init__(self, output_dir: str = None):
        if output_dir is None:
            # 自动检测路径
            if os.path.exists('code/data-collection'):
                self.base_path = 'code'
            else:
                self.base_path = '.'
            self.output_dir = os.path.join(self.base_path, 'data-collection', 'crawled_data')
        else:
            self.output_dir = output_dir
            self.base_path = '.'
        
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 数据存储
        self.exercises_data = []
        self.users_data = []
        self.records_data = []
        self.metrics_data = []
    
    def download_kaggle_datasets(self) -> int:
        """下载Kaggle数据集"""
        print("\n" + "="*60)
        print("下载Kaggle健身数据集")
        print("="*60)
        
        # 检查Kaggle配置
        kaggle_paths = [
            os.path.expanduser('~/.kaggle/kaggle.json'),
            os.path.join(os.environ.get('USERPROFILE', ''), '.kaggle', 'kaggle.json')
        ]
        
        kaggle_configured = any(os.path.exists(p) for p in kaggle_paths)
        
        if not kaggle_configured:
            print("\n⚠️ Kaggle API未配置")
            print("\n配置步骤:")
            print("1. 访问 https://www.kaggle.com/settings")
            print("2. 点击 'Create New API Token'")
            print("3. 下载 kaggle.json")
            print("4. 放到 ~/.kaggle/ 或 C:\\Users\\用户名\\.kaggle\\")
            return 0
        
        downloaded = 0
        kaggle_dir = os.path.join(self.output_dir, 'kaggle')
        os.makedirs(kaggle_dir, exist_ok=True)
        
        for dataset in self.KAGGLE_DATASETS:
            print(f"\n📥 下载: {dataset['name']}")
            print(f"   描述: {dataset['desc']}")
            
            try:
                cmd = f'kaggle datasets download -d {dataset["name"]} -p "{kaggle_dir}" --unzip'
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=300)
                
                if result.returncode == 0:
                    print(f"   ✅ 下载成功")
                    downloaded += 1
                else:
                    print(f"   ❌ 下载失败")
            except subprocess.TimeoutExpired:
                print(f"   ❌ 下载超时")
            except Exception as e:
                print(f"   ❌ 错误: {str(e)[:50]}")
        
        print(f"\n下载完成: {downloaded}/{len(self.KAGGLE_DATASETS)} 个数据集")
        return downloaded
    
    def fetch_wger_exercises(self) -> List[Dict]:
        """从Wger API获取运动数据（免费开源API）"""
        print("\n" + "="*60)
        print("从Wger API获取运动数据")
        print("="*60)
        
        exercises = []
        base_url = "https://wger.de/api/v2"
        
        try:
            # 获取运动列表
            print("正在获取运动数据...")
            response = requests.get(
                f"{base_url}/exercise/",
                params={'language': 2, 'limit': 200},  # 英文
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                for item in data.get('results', []):
                    exercise = {
                        'id': item.get('id'),
                        'name': item.get('name'),
                        'description': item.get('description', ''),
                        'category': item.get('category'),
                        'muscles': item.get('muscles', []),
                        'equipment': item.get('equipment', [])
                    }
                    exercises.append(exercise)
                
                print(f"✅ 获取了 {len(exercises)} 条运动数据")
            else:
                print(f"❌ API请求失败: {response.status_code}")
                
            # 获取运动类别
            print("正在获取运动类别...")
            cat_response = requests.get(f"{base_url}/exercisecategory/", timeout=30)
            if cat_response.status_code == 200:
                categories = {c['id']: c['name'] for c in cat_response.json().get('results', [])}
                for ex in exercises:
                    ex['category_name'] = categories.get(ex['category'], 'Unknown')
                print(f"✅ 获取了 {len(categories)} 个运动类别")
            
            # 获取器材信息
            print("正在获取器材数据...")
            equip_response = requests.get(f"{base_url}/equipment/", timeout=30)
            if equip_response.status_code == 200:
                equipment = {e['id']: e['name'] for e in equip_response.json().get('results', [])}
                for ex in exercises:
                    ex['equipment_names'] = [equipment.get(e, 'Unknown') for e in ex['equipment']]
                print(f"✅ 获取了 {len(equipment)} 种器材")
            
            # 保存数据
            if exercises:
                df = pd.DataFrame(exercises)
                output_path = os.path.join(self.output_dir, 'wger_exercises.csv')
                df.to_csv(output_path, index=False, encoding='utf-8-sig')
                print(f"✅ 数据已保存到: {output_path}")
                
                self.exercises_data.extend(exercises)
                
        except requests.exceptions.Timeout:
            print("❌ 请求超时")
        except requests.exceptions.RequestException as e:
            print(f"❌ 网络错误: {str(e)[:50]}")
        except Exception as e:
            print(f"❌ 错误: {str(e)[:50]}")
        
        return exercises
    
    def fetch_nutritionix_data(self) -> List[Dict]:
        """获取运动卡路里消耗数据（模拟）"""
        print("\n" + "="*60)
        print("生成运动卡路里消耗数据")
        print("="*60)
        
        # 基于真实数据的运动卡路里消耗率（每分钟每公斤体重）
        exercise_calories = [
            {'exercise': '跑步', 'met': 9.8, 'category': '有氧运动'},
            {'exercise': '快走', 'met': 5.0, 'category': '有氧运动'},
            {'exercise': '游泳', 'met': 8.0, 'category': '有氧运动'},
            {'exercise': '动感单车', 'met': 8.5, 'category': '有氧运动'},
            {'exercise': '椭圆机', 'met': 7.0, 'category': '有氧运动'},
            {'exercise': '划船机', 'met': 7.5, 'category': '有氧运动'},
            {'exercise': '爬楼机', 'met': 9.0, 'category': '有氧运动'},
            {'exercise': '跳绳', 'met': 12.0, 'category': '有氧运动'},
            {'exercise': 'HIIT训练', 'met': 12.5, 'category': '有氧运动'},
            {'exercise': '力量训练', 'met': 6.0, 'category': '力量训练'},
            {'exercise': '哑铃训练', 'met': 5.5, 'category': '力量训练'},
            {'exercise': '杠铃训练', 'met': 6.5, 'category': '力量训练'},
            {'exercise': '器械训练', 'met': 5.0, 'category': '力量训练'},
            {'exercise': '瑜伽', 'met': 3.0, 'category': '柔韧训练'},
            {'exercise': '普拉提', 'met': 4.0, 'category': '柔韧训练'},
            {'exercise': '拳击', 'met': 10.0, 'category': '格斗训练'},
            {'exercise': '搏击操', 'met': 9.5, 'category': '格斗训练'},
            {'exercise': '健身操', 'met': 6.5, 'category': '有氧运动'},
            {'exercise': '舞蹈', 'met': 5.5, 'category': '有氧运动'},
            {'exercise': '登山', 'met': 8.0, 'category': '户外运动'},
        ]
        
        # 保存数据
        df = pd.DataFrame(exercise_calories)
        output_path = os.path.join(self.output_dir, 'exercise_calories.csv')
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✅ 生成了 {len(exercise_calories)} 条运动卡路里数据")
        print(f"✅ 数据已保存到: {output_path}")
        
        return exercise_calories
    
    def generate_realistic_users(self, count: int = 1000) -> List[Dict]:
        """生成真实感的用户数据"""
        print("\n" + "="*60)
        print(f"生成 {count} 个真实感用户数据")
        print("="*60)
        
        try:
            from faker import Faker
            fake = Faker('zh_CN')
        except ImportError:
            print("正在安装faker库...")
            subprocess.run([sys.executable, '-m', 'pip', 'install', 'faker', '-q'])
            from faker import Faker
            fake = Faker('zh_CN')
        
        users = []
        
        # 年龄分布（健身房用户年龄分布）
        age_weights = {
            (18, 25): 0.25,  # 年轻人
            (26, 35): 0.35,  # 主力人群
            (36, 45): 0.25,  # 中年人
            (46, 55): 0.10,  # 中老年
            (56, 65): 0.05   # 老年人
        }
        
        # 健身目标分布
        goal_weights = {'减重': 0.40, '减脂': 0.30, '增肌': 0.20, '塑形': 0.10}
        
        for i in range(count):
            # 确定年龄
            age_range = random.choices(
                list(age_weights.keys()),
                weights=list(age_weights.values())
            )[0]
            age = random.randint(age_range[0], age_range[1])
            
            # 确定性别
            gender = random.choice(['男', '女'])
            
            # 根据性别和年龄生成身高体重
            if gender == '男':
                height = np.random.normal(172, 6)  # 男性平均身高172cm
                bmi = np.random.normal(24, 3)  # BMI分布
            else:
                height = np.random.normal(160, 5)  # 女性平均身高160cm
                bmi = np.random.normal(22, 3)
            
            height = max(150, min(195, height))
            bmi = max(17, min(35, bmi))
            weight = bmi * (height / 100) ** 2
            
            # 确定健身目标
            goal = random.choices(
                list(goal_weights.keys()),
                weights=list(goal_weights.values())
            )[0]
            
            # 会员类型
            member_type = random.choices(
                ['月卡', '季卡', '年卡', '次卡'],
                weights=[0.20, 0.30, 0.40, 0.10]
            )[0]
            
            user = {
                'user_id': i + 1,
                'username': fake.user_name() + str(i),
                'real_name': fake.name(),
                'email': fake.email(),
                'phone': fake.phone_number(),
                'age': age,
                'gender': gender,
                'height_cm': round(height, 1),
                'initial_weight_kg': round(weight, 1),
                'bmi': round(bmi, 1),
                'fitness_goal': goal,
                'member_type': member_type,
                'join_date': (datetime.now() - timedelta(days=random.randint(30, 730))).strftime('%Y-%m-%d'),
                'role': 'STUDENT'
            }
            users.append(user)
        
        # 添加教练
        for i in range(30):
            user = {
                'user_id': count + i + 1,
                'username': f'coach_{i+1}',
                'real_name': fake.name(),
                'email': f'coach{i+1}@gym.com',
                'phone': fake.phone_number(),
                'age': random.randint(25, 45),
                'gender': random.choice(['男', '女']),
                'height_cm': random.uniform(165, 185),
                'initial_weight_kg': random.uniform(60, 85),
                'bmi': 22,
                'fitness_goal': None,
                'member_type': '员工',
                'join_date': (datetime.now() - timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d'),
                'role': 'COACH'
            }
            users.append(user)
        
        # 保存数据
        df = pd.DataFrame(users)
        output_path = os.path.join(self.output_dir, 'users_generated.csv')
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✅ 生成了 {len(users)} 个用户数据")
        print(f"✅ 数据已保存到: {output_path}")
        
        self.users_data = users
        return users
    
    def generate_exercise_records(self, users: List[Dict], days: int = 180) -> List[Dict]:
        """生成运动记录数据"""
        print("\n" + "="*60)
        print(f"生成 {days} 天的运动记录数据")
        print("="*60)
        
        records = []
        record_id = 1
        
        exercise_types = [
            {'name': '跑步', 'duration_range': (20, 60), 'cal_per_min': 10},
            {'name': '动感单车', 'duration_range': (30, 60), 'cal_per_min': 12},
            {'name': '游泳', 'duration_range': (30, 60), 'cal_per_min': 11},
            {'name': '力量训练', 'duration_range': (40, 90), 'cal_per_min': 8},
            {'name': '瑜伽', 'duration_range': (45, 75), 'cal_per_min': 4},
            {'name': '椭圆机', 'duration_range': (20, 45), 'cal_per_min': 9},
            {'name': '划船机', 'duration_range': (15, 30), 'cal_per_min': 10},
            {'name': 'HIIT训练', 'duration_range': (20, 40), 'cal_per_min': 13},
            {'name': '拳击', 'duration_range': (30, 60), 'cal_per_min': 12},
            {'name': '普拉提', 'duration_range': (45, 60), 'cal_per_min': 5},
        ]
        
        equipment_map = {
            '跑步': ['跑步机', None],
            '动感单车': ['动感单车'],
            '游泳': [None],
            '力量训练': ['哑铃', '杠铃', '史密斯机', '龙门架'],
            '瑜伽': ['瑜伽垫'],
            '椭圆机': ['椭圆机'],
            '划船机': ['划船机'],
            'HIIT训练': ['跳绳', '壶铃', None],
            '拳击': ['拳击沙袋', None],
            '普拉提': ['瑜伽垫', '健身球'],
        }
        
        students = [u for u in users if u['role'] == 'STUDENT']
        
        for user in students:
            # 根据用户特征确定运动频率
            if user['fitness_goal'] in ['减重', '减脂']:
                weekly_freq = random.randint(4, 6)
            elif user['fitness_goal'] == '增肌':
                weekly_freq = random.randint(4, 5)
            else:
                weekly_freq = random.randint(2, 4)
            
            # 用户偏好的运动类型
            preferred_exercises = random.sample(exercise_types, k=random.randint(2, 4))
            
            # 生成记录
            join_date = datetime.strptime(user['join_date'], '%Y-%m-%d')
            current_date = datetime.now()
            
            date = join_date
            while date <= current_date:
                # 判断这一天是否运动
                if random.random() < weekly_freq / 7:
                    exercise = random.choice(preferred_exercises)
                    duration = random.randint(*exercise['duration_range'])
                    calories = duration * exercise['cal_per_min'] * random.uniform(0.9, 1.1)
                    
                    # 心率
                    base_hr = 220 - user['age']
                    avg_hr = int(base_hr * random.uniform(0.6, 0.8))
                    max_hr = int(base_hr * random.uniform(0.8, 0.95))
                    
                    record = {
                        'record_id': record_id,
                        'user_id': user['user_id'],
                        'exercise_type': exercise['name'],
                        'exercise_date': date.strftime('%Y-%m-%d'),
                        'duration_minutes': duration,
                        'calories_burned': round(calories, 2),
                        'average_heart_rate': avg_hr,
                        'max_heart_rate': max_hr,
                        'equipment_used': random.choice(equipment_map.get(exercise['name'], [None])),
                        'created_at': date.isoformat()
                    }
                    records.append(record)
                    record_id += 1
                
                date += timedelta(days=1)
        
        # 保存数据
        df = pd.DataFrame(records)
        output_path = os.path.join(self.output_dir, 'exercise_records_generated.csv')
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✅ 生成了 {len(records)} 条运动记录")
        print(f"✅ 数据已保存到: {output_path}")
        
        self.records_data = records
        return records
    
    def generate_body_metrics(self, users: List[Dict]) -> List[Dict]:
        """生成身体指标数据"""
        print("\n" + "="*60)
        print("生成身体指标数据")
        print("="*60)
        
        metrics = []
        metric_id = 1
        
        students = [u for u in users if u['role'] == 'STUDENT']
        
        for user in students:
            join_date = datetime.strptime(user['join_date'], '%Y-%m-%d')
            current_date = datetime.now()
            
            # 初始值
            weight = user['initial_weight_kg']
            height = user['height_cm']
            
            # 根据目标确定体脂率
            if user['gender'] == '男':
                body_fat = random.uniform(18, 28)
            else:
                body_fat = random.uniform(22, 32)
            
            # 每周记录一次
            date = join_date
            week_count = 0
            
            while date <= current_date:
                if week_count % 1 == 0:  # 每周记录
                    # 根据健身目标模拟变化
                    if user['fitness_goal'] in ['减重', '减脂']:
                        weight_change = random.uniform(-0.3, 0.1)
                        fat_change = random.uniform(-0.2, 0.05)
                    elif user['fitness_goal'] == '增肌':
                        weight_change = random.uniform(-0.1, 0.3)
                        fat_change = random.uniform(-0.15, 0.1)
                    else:
                        weight_change = random.uniform(-0.2, 0.2)
                        fat_change = random.uniform(-0.1, 0.1)
                    
                    weight = max(40, min(120, weight + weight_change))
                    body_fat = max(10, min(40, body_fat + fat_change))
                    
                    bmi = weight / ((height / 100) ** 2)
                    muscle_mass = weight * (1 - body_fat / 100) * random.uniform(0.4, 0.5)
                    
                    metric = {
                        'metric_id': metric_id,
                        'user_id': user['user_id'],
                        'measurement_date': date.strftime('%Y-%m-%d'),
                        'weight_kg': round(weight, 2),
                        'body_fat_percentage': round(body_fat, 2),
                        'height_cm': height,
                        'bmi': round(bmi, 2),
                        'muscle_mass_kg': round(muscle_mass, 2),
                        'created_at': date.isoformat()
                    }
                    metrics.append(metric)
                    metric_id += 1
                
                date += timedelta(days=7)
                week_count += 1
        
        # 保存数据
        df = pd.DataFrame(metrics)
        output_path = os.path.join(self.output_dir, 'body_metrics_generated.csv')
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✅ 生成了 {len(metrics)} 条身体指标数据")
        print(f"✅ 数据已保存到: {output_path}")
        
        self.metrics_data = metrics
        return metrics
    
    def merge_all_data(self):
        """合并所有数据到cleaned目录"""
        print("\n" + "="*60)
        print("合并所有数据")
        print("="*60)
        
        cleaned_dir = os.path.join(self.base_path, 'data-processing', 'cleaned')
        os.makedirs(cleaned_dir, exist_ok=True)
        
        # 合并用户数据
        if self.users_data:
            df = pd.DataFrame(self.users_data)
            output_path = os.path.join(cleaned_dir, 'users.csv')
            df.to_csv(output_path, index=False, encoding='utf-8-sig')
            print(f"✅ 用户数据: {len(df)} 条 -> {output_path}")
        
        # 合并运动记录
        if self.records_data:
            df = pd.DataFrame(self.records_data)
            output_path = os.path.join(cleaned_dir, 'exercise_records.csv')
            df.to_csv(output_path, index=False, encoding='utf-8-sig')
            print(f"✅ 运动记录: {len(df)} 条 -> {output_path}")
        
        # 合并身体指标
        if self.metrics_data:
            df = pd.DataFrame(self.metrics_data)
            output_path = os.path.join(cleaned_dir, 'body_metrics.csv')
            df.to_csv(output_path, index=False, encoding='utf-8-sig')
            print(f"✅ 身体指标: {len(df)} 条 -> {output_path}")
    
    def print_summary(self):
        """打印数据统计"""
        print("\n" + "="*60)
        print("数据采集统计")
        print("="*60)
        
        print(f"\n用户数据: {len(self.users_data)} 条")
        if self.users_data:
            students = [u for u in self.users_data if u['role'] == 'STUDENT']
            coaches = [u for u in self.users_data if u['role'] == 'COACH']
            print(f"  - 学员: {len(students)}")
            print(f"  - 教练: {len(coaches)}")
        
        print(f"\n运动记录: {len(self.records_data)} 条")
        if self.records_data:
            types = {}
            for r in self.records_data:
                t = r['exercise_type']
                types[t] = types.get(t, 0) + 1
            print("  运动类型分布:")
            for t, c in sorted(types.items(), key=lambda x: -x[1])[:5]:
                print(f"    - {t}: {c}")
        
        print(f"\n身体指标: {len(self.metrics_data)} 条")
        
        print(f"\n运动参考: {len(self.exercises_data)} 条")
    
    def run(self, 
            download_kaggle: bool = False,
            fetch_api: bool = True,
            generate_users: int = 1000,
            generate_days: int = 180):
        """运行完整的数据采集流程"""
        print("="*60)
        print("健身数据多源采集器")
        print("="*60)
        print(f"输出目录: {self.output_dir}")
        
        # 1. 可选：下载Kaggle数据集
        if download_kaggle:
            self.download_kaggle_datasets()
        
        # 2. 从公开API获取数据
        if fetch_api:
            self.fetch_wger_exercises()
            self.fetch_nutritionix_data()
        
        # 3. 生成用户数据
        if generate_users > 0:
            users = self.generate_realistic_users(generate_users)
            
            # 4. 生成运动记录
            self.generate_exercise_records(users, generate_days)
            
            # 5. 生成身体指标
            self.generate_body_metrics(users)
        
        # 6. 合并数据
        self.merge_all_data()
        
        # 7. 打印统计
        self.print_summary()
        
        print("\n" + "="*60)
        print("数据采集完成！")
        print("="*60)
        print(f"\n数据位置:")
        print(f"  - 原始数据: {self.output_dir}")
        print(f"  - 清洗数据: {os.path.join(self.base_path, 'data-processing', 'cleaned')}")


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='健身数据多源采集器')
    parser.add_argument('--kaggle', action='store_true', help='下载Kaggle数据集')
    parser.add_argument('--api', action='store_true', default=True, help='从公开API获取数据')
    parser.add_argument('--users', type=int, default=1000, help='生成用户数量')
    parser.add_argument('--days', type=int, default=180, help='生成运动记录天数')
    parser.add_argument('--output', type=str, default=None, help='输出目录')
    
    args = parser.parse_args()
    
    crawler = FitnessDataCrawler(output_dir=args.output)
    crawler.run(
        download_kaggle=args.kaggle,
        fetch_api=args.api,
        generate_users=args.users,
        generate_days=args.days
    )


if __name__ == '__main__':
    main()

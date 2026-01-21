"""
增强版健身数据集收集器
支持多种数据源：Kaggle、现有CSV、模拟数据生成
"""
import os
import sys
import json
import random
import subprocess
from datetime import datetime, timedelta
from typing import List, Dict, Optional
import pandas as pd
import numpy as np

# 设置随机种子确保可重复性
random.seed(42)
np.random.seed(42)

class EnhancedDatasetCollector:
    """增强版数据集收集器"""
    
    # 推荐的Kaggle健身数据集
    KAGGLE_DATASETS = [
        {
            'name': 'aroojanwarkhan/fitness-data-trends',
            'description': '健身趋势数据',
            'type': 'fitness_trends'
        },
        {
            'name': 'kukuroo3/body-performance-data',
            'description': '身体表现数据',
            'type': 'body_performance'
        },
        {
            'name': 'valakhorasani/gym-members-exercise-dataset',
            'description': '健身房会员运动数据',
            'type': 'gym_members'
        },
        {
            'name': 'niharika41298/gym-exercise-data',
            'description': '健身房运动数据',
            'type': 'gym_exercise'
        },
        {
            'name': 'aakashjoshi123/exercise-and-fitness-metrics-dataset',
            'description': '运动和健身指标数据',
            'type': 'fitness_metrics'
        },
        {
            'name': 'rishikeshkonapure/fitness-trackers-products-ecommerce',
            'description': '健身追踪器产品数据',
            'type': 'fitness_products'
        }
    ]
    
    # 运动类型映射
    EXERCISE_TYPE_MAP = {
        'running': '跑步',
        'cycling': '动感单车',
        'swimming': '游泳',
        'strength': '力量训练',
        'yoga': '瑜伽',
        'pilates': '普拉提',
        'elliptical': '椭圆机',
        'rowing': '划船机',
        'stair': '爬楼机',
        'boxing': '拳击',
        'walking': '散步',
        'hiit': 'HIIT训练',
        'cardio': '有氧运动',
        'weight': '举重',
        'aerobics': '健身操'
    }
    
    # 器材类型
    EQUIPMENT_TYPES = [
        '跑步机', '动感单车', '哑铃', '杠铃', '史密斯机',
        '龙门架', '椭圆机', '划船机', '瑜伽垫', '拳击沙袋',
        '壶铃', '弹力带', '健身球', '引体向上架', '腿举机'
    ]
    
    def __init__(self, base_path: str = None):
        if base_path is None:
            # 自动检测路径
            if os.path.exists('code/csv'):
                self.base_path = 'code'
            elif os.path.exists('csv'):
                self.base_path = '.'
            else:
                self.base_path = '.'
        else:
            self.base_path = base_path
            
        self.csv_path = os.path.join(self.base_path, 'csv')
        self.output_path = os.path.join(self.base_path, 'data-collection', 'output')
        self.cleaned_path = os.path.join(self.base_path, 'data-processing', 'cleaned')
        
        # 确保输出目录存在
        os.makedirs(self.output_path, exist_ok=True)
        os.makedirs(self.cleaned_path, exist_ok=True)
        
        # 数据存储
        self.users = []
        self.exercise_records = []
        self.body_metrics = []
        self.exercise_reference = []
        
        # ID计数器
        self.user_id_counter = 1
        self.record_id_counter = 1
        self.metric_id_counter = 1
    
    def check_kaggle_setup(self) -> bool:
        """检查Kaggle API配置"""
        kaggle_paths = [
            os.path.expanduser('~/.kaggle/kaggle.json'),
            os.path.join(os.environ.get('USERPROFILE', ''), '.kaggle', 'kaggle.json')
        ]
        
        for path in kaggle_paths:
            if os.path.exists(path):
                print(f"✅ 找到Kaggle配置: {path}")
                return True
        
        print("⚠️ 未找到Kaggle API配置")
        print("\n配置步骤:")
        print("1. 访问 https://www.kaggle.com/settings")
        print("2. 点击 'Create New API Token'")
        print("3. 下载 kaggle.json")
        print("4. 放到 ~/.kaggle/ 或 C:\\Users\\用户名\\.kaggle\\")
        return False
    
    def download_kaggle_dataset(self, dataset_info: Dict) -> Optional[str]:
        """下载Kaggle数据集"""
        dataset_name = dataset_info['name']
        output_dir = os.path.join(self.output_path, 'kaggle', dataset_info['type'])
        os.makedirs(output_dir, exist_ok=True)
        
        print(f"\n📥 下载: {dataset_name}")
        print(f"   描述: {dataset_info['description']}")
        
        try:
            cmd = f'kaggle datasets download -d {dataset_name} -p "{output_dir}" --unzip'
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=300)
            
            if result.returncode == 0:
                print(f"   ✅ 下载成功")
                return output_dir
            else:
                print(f"   ❌ 下载失败: {result.stderr[:100]}")
                return None
        except subprocess.TimeoutExpired:
            print(f"   ❌ 下载超时")
            return None
        except Exception as e:
            print(f"   ❌ 错误: {str(e)}")
            return None
    
    def download_all_kaggle_datasets(self) -> List[str]:
        """下载所有Kaggle数据集"""
        if not self.check_kaggle_setup():
            return []
        
        print("\n" + "="*60)
        print("开始下载Kaggle健身数据集")
        print("="*60)
        
        downloaded = []
        for dataset in self.KAGGLE_DATASETS:
            path = self.download_kaggle_dataset(dataset)
            if path:
                downloaded.append(path)
        
        print(f"\n下载完成: {len(downloaded)}/{len(self.KAGGLE_DATASETS)} 个数据集")
        return downloaded
    
    def process_fitbit_data(self):
        """处理Fitbit数据"""
        print("\n" + "="*60)
        print("处理Fitbit数据")
        print("="*60)
        
        fitbit_dirs = [
            os.path.join(self.csv_path, 'mturkfitbit_export_3.12.16-4.11.16'),
            os.path.join(self.csv_path, 'mturkfitbit_export_4.12.16-5.12.16')
        ]
        
        fitbit_user_map = {}  # Fitbit ID -> 系统用户ID
        
        for fitbit_dir in fitbit_dirs:
            if not os.path.exists(fitbit_dir):
                continue
                
            # 查找数据文件
            for root, dirs, files in os.walk(fitbit_dir):
                for file in files:
                    file_path = os.path.join(root, file)
                    file_lower = file.lower()
                    
                    if 'dailyactivity' in file_lower:
                        self._process_fitbit_daily_activity(file_path, fitbit_user_map)
                    elif 'weightlog' in file_lower:
                        self._process_fitbit_weight_log(file_path, fitbit_user_map)
        
        # 为Fitbit用户创建用户记录
        self._create_fitbit_users(fitbit_user_map)
        
        print(f"✅ 处理了 {len(fitbit_user_map)} 个Fitbit用户")
    
    def _process_fitbit_daily_activity(self, file_path: str, user_map: Dict):
        """处理Fitbit日常活动数据"""
        print(f"  处理: {os.path.basename(file_path)}")
        
        try:
            df = pd.read_csv(file_path)
            df = df.drop_duplicates()
            
            for _, row in df.iterrows():
                try:
                    fitbit_id = str(row['Id'])
                    
                    # 获取或创建用户ID
                    if fitbit_id not in user_map:
                        user_map[fitbit_id] = self.user_id_counter
                        self.user_id_counter += 1
                    user_id = user_map[fitbit_id]
                    
                    # 解析日期
                    activity_date = pd.to_datetime(row['ActivityDate'])
                    
                    # 获取活动数据
                    very_active = int(row.get('VeryActiveMinutes', 0) or 0)
                    fairly_active = int(row.get('FairlyActiveMinutes', 0) or 0)
                    lightly_active = int(row.get('LightlyActiveMinutes', 0) or 0)
                    calories = float(row.get('Calories', 0) or 0)
                    
                    # 确定运动类型和时长
                    if very_active >= 30:
                        exercise_type = '跑步'
                        duration = very_active
                        avg_hr = random.randint(140, 170)
                        max_hr = random.randint(170, 190)
                    elif fairly_active >= 20:
                        exercise_type = '快走'
                        duration = fairly_active
                        avg_hr = random.randint(110, 140)
                        max_hr = random.randint(140, 165)
                    elif lightly_active >= 30:
                        exercise_type = '散步'
                        duration = min(lightly_active, 60)
                        avg_hr = random.randint(90, 110)
                        max_hr = random.randint(110, 130)
                    else:
                        continue
                    
                    record = {
                        'record_id': self.record_id_counter,
                        'user_id': user_id,
                        'exercise_type': exercise_type,
                        'exercise_date': activity_date.strftime('%Y-%m-%d'),
                        'duration_minutes': duration,
                        'calories_burned': round(calories * duration / (very_active + fairly_active + lightly_active + 1), 2),
                        'average_heart_rate': avg_hr,
                        'max_heart_rate': max_hr,
                        'equipment_used': random.choice(['跑步机', None, None]),
                        'created_at': datetime.now().isoformat()
                    }
                    self.exercise_records.append(record)
                    self.record_id_counter += 1
                    
                except Exception:
                    continue
                    
        except Exception as e:
            print(f"    ❌ 错误: {e}")
    
    def _process_fitbit_weight_log(self, file_path: str, user_map: Dict):
        """处理Fitbit体重数据"""
        print(f"  处理: {os.path.basename(file_path)}")
        
        try:
            df = pd.read_csv(file_path)
            df = df.drop_duplicates()
            
            for _, row in df.iterrows():
                try:
                    fitbit_id = str(row['Id'])
                    
                    if fitbit_id not in user_map:
                        user_map[fitbit_id] = self.user_id_counter
                        self.user_id_counter += 1
                    user_id = user_map[fitbit_id]
                    
                    measurement_date = pd.to_datetime(row['Date'])
                    weight_kg = float(row['WeightKg'])
                    bmi = float(row.get('BMI', 0) or 0)
                    body_fat = float(row.get('Fat', 0) or 0) if pd.notna(row.get('Fat')) else None
                    
                    # 估算身高
                    if bmi > 0:
                        height_cm = np.sqrt(weight_kg / bmi) * 100
                    else:
                        height_cm = random.uniform(160, 180)
                        bmi = weight_kg / ((height_cm / 100) ** 2)
                    
                    # 估算肌肉量
                    if body_fat:
                        muscle_mass = weight_kg * (1 - body_fat / 100) * 0.45
                    else:
                        body_fat = random.uniform(18, 30)
                        muscle_mass = weight_kg * (1 - body_fat / 100) * 0.45
                    
                    metric = {
                        'metric_id': self.metric_id_counter,
                        'user_id': user_id,
                        'measurement_date': measurement_date.strftime('%Y-%m-%d'),
                        'weight_kg': round(weight_kg, 2),
                        'body_fat_percentage': round(body_fat, 2) if body_fat else None,
                        'height_cm': round(height_cm, 2),
                        'bmi': round(bmi, 2),
                        'muscle_mass_kg': round(muscle_mass, 2),
                        'created_at': datetime.now().isoformat()
                    }
                    self.body_metrics.append(metric)
                    self.metric_id_counter += 1
                    
                except Exception:
                    continue
                    
        except Exception as e:
            print(f"    ❌ 错误: {e}")
    
    def _create_fitbit_users(self, user_map: Dict):
        """为Fitbit用户创建用户记录"""
        from faker import Faker
        fake = Faker('zh_CN')
        
        for fitbit_id, user_id in user_map.items():
            gender = random.choice(['男', '女'])
            age = random.randint(20, 55)
            
            if gender == '男':
                height = random.uniform(165, 185)
                weight = random.uniform(60, 90)
            else:
                height = random.uniform(155, 170)
                weight = random.uniform(45, 70)
            
            user = {
                'user_id': user_id,
                'username': f'fitbit_{user_id}',
                'real_name': fake.name(),
                'email': f'fitbit{user_id}@gym.com',
                'phone': fake.phone_number(),
                'age': age,
                'gender': gender,
                'height_cm': round(height, 1),
                'initial_weight_kg': round(weight, 1),
                'fitness_goal': random.choice(['减重', '减脂', '增肌']),
                'role': 'STUDENT',
                'created_at': (datetime.now() - timedelta(days=random.randint(180, 365))).isoformat()
            }
            self.users.append(user)
    
    def process_mega_gym_dataset(self):
        """处理MegaGym运动数据集"""
        print("\n" + "="*60)
        print("处理MegaGym运动数据集")
        print("="*60)
        
        file_path = os.path.join(self.csv_path, 'megaGymDataset.csv')
        if not os.path.exists(file_path):
            print(f"  ⚠️ 文件不存在: {file_path}")
            return
        
        try:
            df = pd.read_csv(file_path)
            print(f"  读取 {len(df)} 条运动参考数据")
            
            for _, row in df.iterrows():
                try:
                    exercise_ref = {
                        'title': row.get('Title', ''),
                        'description': row.get('Desc', ''),
                        'type': row.get('Type', ''),
                        'body_part': row.get('BodyPart', ''),
                        'equipment': row.get('Equipment', ''),
                        'level': row.get('Level', ''),
                        'rating': row.get('Rating', 0)
                    }
                    self.exercise_reference.append(exercise_ref)
                except Exception:
                    continue
            
            print(f"  ✅ 处理了 {len(self.exercise_reference)} 条运动参考")
            
        except Exception as e:
            print(f"  ❌ 错误: {e}")
    
    def process_fitness_analysis_survey(self):
        """处理健身分析调查数据"""
        print("\n" + "="*60)
        print("处理健身分析调查数据")
        print("="*60)
        
        file_path = os.path.join(self.csv_path, 'fitness analysis.csv')
        if not os.path.exists(file_path):
            print(f"  ⚠️ 文件不存在: {file_path}")
            return
        
        try:
            df = pd.read_csv(file_path)
            print(f"  读取 {len(df)} 条调查数据")
            
            for _, row in df.iterrows():
                try:
                    name = str(row.get('Your name ', '')).strip()
                    gender = '男' if row.get('Your gender ') == 'Male' else '女'
                    age_range = str(row.get('Your age ', '19 to 25'))
                    
                    # 解析年龄
                    if '15 to 18' in age_range:
                        age = random.randint(15, 18)
                    elif '19 to 25' in age_range:
                        age = random.randint(19, 25)
                    elif '30 to 40' in age_range:
                        age = random.randint(30, 40)
                    elif '40 and above' in age_range:
                        age = random.randint(40, 55)
                    else:
                        age = random.randint(20, 35)
                    
                    # 生成身高体重
                    if gender == '男':
                        height = random.uniform(165, 185)
                        weight = random.uniform(60, 90)
                    else:
                        height = random.uniform(155, 170)
                        weight = random.uniform(45, 70)
                    
                    # 解析健身目标
                    motivation = str(row.get('What motivates you to exercise?         (Please select all that applies )', ''))
                    if 'lose weight' in motivation.lower():
                        goal = '减重'
                    elif 'muscle' in motivation.lower():
                        goal = '增肌'
                    else:
                        goal = random.choice(['减重', '减脂', '增肌'])
                    
                    user = {
                        'user_id': self.user_id_counter,
                        'username': f'survey_{self.user_id_counter}',
                        'real_name': name if name else f'调查用户{self.user_id_counter}',
                        'email': f'survey{self.user_id_counter}@gym.com',
                        'phone': f'138{random.randint(10000000, 99999999)}',
                        'age': age,
                        'gender': gender,
                        'height_cm': round(height, 1),
                        'initial_weight_kg': round(weight, 1),
                        'fitness_goal': goal,
                        'role': 'STUDENT',
                        'created_at': (datetime.now() - timedelta(days=random.randint(30, 365))).isoformat()
                    }
                    self.users.append(user)
                    self.user_id_counter += 1
                    
                    # 生成运动记录
                    exercise_freq = str(row.get('How often do you exercise?', ''))
                    if 'Never' not in exercise_freq:
                        self._generate_exercise_for_user(user, exercise_freq)
                    
                except Exception:
                    continue
            
            print(f"  ✅ 处理了 {len(df)} 条调查数据")
            
        except Exception as e:
            print(f"  ❌ 错误: {e}")
    
    def _generate_exercise_for_user(self, user: Dict, freq: str):
        """为用户生成运动记录"""
        # 根据频率确定记录数
        if 'Everyday' in freq:
            num_records = random.randint(25, 35)
        elif '5 to 6' in freq:
            num_records = random.randint(20, 28)
        elif '3 to 4' in freq:
            num_records = random.randint(12, 20)
        elif '2 to 3' in freq:
            num_records = random.randint(8, 15)
        elif '1 to 2' in freq:
            num_records = random.randint(4, 10)
        else:
            num_records = random.randint(1, 5)
        
        exercise_types = ['跑步', '动感单车', '游泳', '力量训练', '瑜伽', '普拉提', '椭圆机']
        
        for i in range(num_records):
            exercise_type = random.choice(exercise_types)
            duration = random.randint(20, 90)
            
            # 根据运动类型计算卡路里
            calorie_rates = {
                '跑步': 10, '动感单车': 12, '游泳': 11, '力量训练': 8,
                '瑜伽': 4, '普拉提': 5, '椭圆机': 9
            }
            calories = duration * calorie_rates.get(exercise_type, 8) * random.uniform(0.9, 1.1)
            
            record = {
                'record_id': self.record_id_counter,
                'user_id': user['user_id'],
                'exercise_type': exercise_type,
                'exercise_date': (datetime.now() - timedelta(days=random.randint(1, 90))).strftime('%Y-%m-%d'),
                'duration_minutes': duration,
                'calories_burned': round(calories, 2),
                'average_heart_rate': random.randint(110, 160),
                'max_heart_rate': random.randint(150, 185),
                'equipment_used': random.choice(self.EQUIPMENT_TYPES),
                'created_at': datetime.now().isoformat()
            }
            self.exercise_records.append(record)
            self.record_id_counter += 1
    
    def generate_synthetic_data(self, num_users: int = 500):
        """生成合成数据补充数据集"""
        print("\n" + "="*60)
        print(f"生成 {num_users} 个合成用户数据")
        print("="*60)
        
        try:
            from faker import Faker
            fake = Faker('zh_CN')
        except ImportError:
            print("  ⚠️ 需要安装faker: pip install faker")
            return
        
        for i in range(num_users):
            gender = random.choice(['男', '女'])
            age = random.randint(18, 55)
            
            if gender == '男':
                height = random.uniform(165, 185)
                weight = random.uniform(60, 95)
            else:
                height = random.uniform(155, 175)
                weight = random.uniform(45, 75)
            
            user = {
                'user_id': self.user_id_counter,
                'username': fake.user_name() + str(self.user_id_counter),
                'real_name': fake.name(),
                'email': fake.email(),
                'phone': fake.phone_number(),
                'age': age,
                'gender': gender,
                'height_cm': round(height, 1),
                'initial_weight_kg': round(weight, 1),
                'fitness_goal': random.choice(['减重', '减脂', '增肌']),
                'role': 'STUDENT',
                'created_at': (datetime.now() - timedelta(days=random.randint(30, 365))).isoformat()
            }
            self.users.append(user)
            
            # 生成运动记录
            num_records = random.randint(30, 70)
            self._generate_exercise_records_for_user(user, num_records)
            
            # 生成身体指标
            num_metrics = random.randint(10, 25)
            self._generate_body_metrics_for_user(user, num_metrics)
            
            self.user_id_counter += 1
        
        # 添加教练和管理员
        self._add_coaches_and_admins(fake)
        
        print(f"  ✅ 生成了 {num_users} 个用户及相关数据")
    
    def _generate_exercise_records_for_user(self, user: Dict, num_records: int):
        """为用户生成运动记录"""
        exercise_types = ['跑步', '动感单车', '游泳', '力量训练', '瑜伽', '普拉提', '椭圆机', '划船机', '爬楼机', '拳击']
        
        created_date = datetime.fromisoformat(user['created_at'].replace('Z', '+00:00').split('+')[0])
        
        for _ in range(num_records):
            exercise_type = random.choice(exercise_types)
            duration = random.randint(20, 120)
            
            calorie_rates = {
                '跑步': 10, '动感单车': 12, '游泳': 11, '力量训练': 8,
                '瑜伽': 4, '普拉提': 5, '椭圆机': 9, '划船机': 10,
                '爬楼机': 11, '拳击': 13
            }
            calories = duration * calorie_rates.get(exercise_type, 8) * random.uniform(0.9, 1.1)
            
            days_since = (datetime.now() - created_date).days
            exercise_date = created_date + timedelta(days=random.randint(0, max(1, days_since)))
            
            record = {
                'record_id': self.record_id_counter,
                'user_id': user['user_id'],
                'exercise_type': exercise_type,
                'exercise_date': exercise_date.strftime('%Y-%m-%d'),
                'duration_minutes': duration,
                'calories_burned': round(calories, 2),
                'average_heart_rate': random.randint(110, 160),
                'max_heart_rate': random.randint(150, 185),
                'equipment_used': random.choice(self.EQUIPMENT_TYPES),
                'created_at': exercise_date.isoformat()
            }
            self.exercise_records.append(record)
            self.record_id_counter += 1
    
    def _generate_body_metrics_for_user(self, user: Dict, num_metrics: int):
        """为用户生成身体指标"""
        created_date = datetime.fromisoformat(user['created_at'].replace('Z', '+00:00').split('+')[0])
        current_weight = user['initial_weight_kg']
        height = user['height_cm']
        goal = user['fitness_goal']
        
        # 初始体脂率
        if user['gender'] == '男':
            body_fat = random.uniform(18, 28)
        else:
            body_fat = random.uniform(22, 32)
        
        days_since = (datetime.now() - created_date).days
        
        for i in range(num_metrics):
            measurement_date = created_date + timedelta(days=int(i * days_since / num_metrics))
            
            # 根据目标模拟变化
            if goal in ['减重', '减脂']:
                weight_change = random.uniform(-0.3, 0.1)
                fat_change = random.uniform(-0.2, 0.05)
            else:  # 增肌
                weight_change = random.uniform(-0.1, 0.3)
                fat_change = random.uniform(-0.15, 0.1)
            
            current_weight = max(40, min(120, current_weight + weight_change))
            body_fat = max(10, min(40, body_fat + fat_change))
            
            bmi = current_weight / ((height / 100) ** 2)
            muscle_mass = current_weight * (1 - body_fat / 100) * random.uniform(0.4, 0.5)
            
            metric = {
                'metric_id': self.metric_id_counter,
                'user_id': user['user_id'],
                'measurement_date': measurement_date.strftime('%Y-%m-%d'),
                'weight_kg': round(current_weight, 2),
                'body_fat_percentage': round(body_fat, 2),
                'height_cm': height,
                'bmi': round(bmi, 2),
                'muscle_mass_kg': round(muscle_mass, 2),
                'created_at': measurement_date.isoformat()
            }
            self.body_metrics.append(metric)
            self.metric_id_counter += 1
    
    def _add_coaches_and_admins(self, fake):
        """添加教练和管理员"""
        # 添加20个教练
        for i in range(20):
            user = {
                'user_id': self.user_id_counter,
                'username': f'coach_{self.user_id_counter}',
                'real_name': fake.name(),
                'email': f'coach{self.user_id_counter}@gym.com',
                'phone': fake.phone_number(),
                'age': random.randint(25, 45),
                'gender': random.choice(['男', '女']),
                'height_cm': random.uniform(165, 185),
                'initial_weight_kg': random.uniform(60, 85),
                'fitness_goal': None,
                'role': 'COACH',
                'created_at': (datetime.now() - timedelta(days=random.randint(365, 730))).isoformat()
            }
            self.users.append(user)
            self.user_id_counter += 1
        
        # 添加管理员
        admin = {
            'user_id': self.user_id_counter,
            'username': 'admin',
            'real_name': '系统管理员',
            'email': 'admin@gym.com',
            'phone': '13800138000',
            'age': 35,
            'gender': '男',
            'height_cm': 175,
            'initial_weight_kg': 70,
            'fitness_goal': None,
            'role': 'ADMIN',
            'created_at': (datetime.now() - timedelta(days=730)).isoformat()
        }
        self.users.append(admin)
        self.user_id_counter += 1
    
    def save_data(self):
        """保存处理后的数据"""
        print("\n" + "="*60)
        print("保存数据")
        print("="*60)
        
        # 保存用户数据
        users_df = pd.DataFrame(self.users)
        users_df.to_csv(os.path.join(self.cleaned_path, 'users.csv'), index=False, encoding='utf-8-sig')
        print(f"  ✅ 用户数据: {len(users_df)} 条")
        
        # 保存运动记录
        exercise_df = pd.DataFrame(self.exercise_records)
        exercise_df.to_csv(os.path.join(self.cleaned_path, 'exercise_records.csv'), index=False, encoding='utf-8-sig')
        print(f"  ✅ 运动记录: {len(exercise_df)} 条")
        
        # 保存身体指标
        metrics_df = pd.DataFrame(self.body_metrics)
        metrics_df.to_csv(os.path.join(self.cleaned_path, 'body_metrics.csv'), index=False, encoding='utf-8-sig')
        print(f"  ✅ 身体指标: {len(metrics_df)} 条")
        
        # 保存运动参考
        if self.exercise_reference:
            ref_df = pd.DataFrame(self.exercise_reference)
            ref_df.to_csv(os.path.join(self.cleaned_path, 'exercise_reference.csv'), index=False, encoding='utf-8-sig')
            print(f"  ✅ 运动参考: {len(ref_df)} 条")
        
        # 保存JSON格式
        with open(os.path.join(self.output_path, 'users.json'), 'w', encoding='utf-8') as f:
            json.dump(self.users, f, ensure_ascii=False, indent=2)
        
        with open(os.path.join(self.output_path, 'exercise_records.json'), 'w', encoding='utf-8') as f:
            json.dump(self.exercise_records, f, ensure_ascii=False, indent=2)
        
        with open(os.path.join(self.output_path, 'body_metrics.json'), 'w', encoding='utf-8') as f:
            json.dump(self.body_metrics, f, ensure_ascii=False, indent=2)
        
        print(f"\n  数据已保存到:")
        print(f"    - {self.cleaned_path}")
        print(f"    - {self.output_path}")
    
    def print_statistics(self):
        """打印数据统计"""
        print("\n" + "="*60)
        print("数据统计")
        print("="*60)
        
        # 用户统计
        students = [u for u in self.users if u['role'] == 'STUDENT']
        coaches = [u for u in self.users if u['role'] == 'COACH']
        admins = [u for u in self.users if u['role'] == 'ADMIN']
        
        print(f"\n用户统计:")
        print(f"  总用户数: {len(self.users)}")
        print(f"  - 学员: {len(students)}")
        print(f"  - 教练: {len(coaches)}")
        print(f"  - 管理员: {len(admins)}")
        
        # 运动记录统计
        print(f"\n运动记录统计:")
        print(f"  总记录数: {len(self.exercise_records)}")
        if self.exercise_records:
            exercise_types = {}
            for r in self.exercise_records:
                t = r['exercise_type']
                exercise_types[t] = exercise_types.get(t, 0) + 1
            print(f"  运动类型分布:")
            for t, c in sorted(exercise_types.items(), key=lambda x: -x[1])[:5]:
                print(f"    - {t}: {c}")
        
        # 身体指标统计
        print(f"\n身体指标统计:")
        print(f"  总记录数: {len(self.body_metrics)}")
        
        # 运动参考统计
        if self.exercise_reference:
            print(f"\n运动参考统计:")
            print(f"  总记录数: {len(self.exercise_reference)}")
    
    def run(self, download_kaggle: bool = False, generate_synthetic: int = 500):
        """运行完整的数据收集流程"""
        print("="*60)
        print("健身数据集收集器")
        print("="*60)
        print(f"基础路径: {self.base_path}")
        print(f"CSV路径: {self.csv_path}")
        
        # 1. 可选：下载Kaggle数据集
        if download_kaggle:
            self.download_all_kaggle_datasets()
        
        # 2. 处理Fitbit数据
        self.process_fitbit_data()
        
        # 3. 处理MegaGym数据集
        self.process_mega_gym_dataset()
        
        # 4. 处理健身调查数据
        self.process_fitness_analysis_survey()
        
        # 5. 生成合成数据
        if generate_synthetic > 0:
            self.generate_synthetic_data(generate_synthetic)
        
        # 6. 保存数据
        self.save_data()
        
        # 7. 打印统计
        self.print_statistics()
        
        print("\n" + "="*60)
        print("数据收集完成！")
        print("="*60)


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='健身数据集收集器')
    parser.add_argument('--kaggle', action='store_true', help='下载Kaggle数据集')
    parser.add_argument('--synthetic', type=int, default=500, help='生成合成用户数量')
    parser.add_argument('--path', type=str, default=None, help='基础路径')
    
    args = parser.parse_args()
    
    collector = EnhancedDatasetCollector(base_path=args.path)
    collector.run(download_kaggle=args.kaggle, generate_synthetic=args.synthetic)


if __name__ == '__main__':
    main()

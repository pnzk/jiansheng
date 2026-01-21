"""
Kaggle健身数据集下载器
需要配置Kaggle API密钥
"""
import os
import json
import subprocess

class KaggleDatasetDownloader:
    """Kaggle数据集下载器"""
    
    def __init__(self):
        self.datasets = [
            'aroojanwarkhan/fitness-data-trends',
            'kukuroo3/body-performance-data',
            'valakhorasani/gym-members-exercise-dataset',
        ]
    
    def check_kaggle_setup(self):
        """检查Kaggle API是否配置"""
        kaggle_json = os.path.expanduser('~/.kaggle/kaggle.json')
        if not os.path.exists(kaggle_json):
            print("❌ 未找到Kaggle API配置")
            print("\n请按以下步骤配置:")
            print("1. 访问 https://www.kaggle.com/settings")
            print("2. 点击 'Create New API Token'")
            print("3. 下载 kaggle.json 文件")
            print("4. 将文件放到: ~/.kaggle/kaggle.json")
            print("5. Windows用户: C:\\Users\\你的用户名\\.kaggle\\kaggle.json")
            return False
        
        print("✅ Kaggle API已配置")
        return True
    
    def download_dataset(self, dataset_name, output_dir='code/data-collection/kaggle_data'):
        """下载指定数据集"""
        os.makedirs(output_dir, exist_ok=True)
        
        print(f"\n正在下载数据集: {dataset_name}")
        
        try:
            # 使用kaggle命令行工具下载
            cmd = f'kaggle datasets download -d {dataset_name} -p {output_dir} --unzip'
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            
            if result.returncode == 0:
                print(f"✅ 下载成功: {dataset_name}")
                return True
            else:
                print(f"❌ 下载失败: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ 下载出错: {str(e)}")
            return False
    
    def download_all(self):
        """下载所有推荐的健身数据集"""
        if not self.check_kaggle_setup():
            return False
        
        print("\n开始下载Kaggle健身数据集...")
        print("=" * 50)
        
        success_count = 0
        for dataset in self.datasets:
            if self.download_dataset(dataset):
                success_count += 1
        
        print("\n" + "=" * 50)
        print(f"下载完成: {success_count}/{len(self.datasets)} 个数据集")
        
        return success_count > 0

def main():
    """主函数"""
    print("Kaggle健身数据集下载工具")
    print("=" * 50)
    
    downloader = KaggleDatasetDownloader()
    
    # 检查并安装kaggle包
    try:
        import kaggle
        print("✅ Kaggle包已安装")
    except ImportError:
        print("正在安装Kaggle包...")
        subprocess.run('pip install kaggle', shell=True)
    
    # 下载数据集
    if downloader.download_all():
        print("\n✅ 数据下载成功！")
        print("数据位置: code/data-collection/kaggle_data/")
        print("\n下一步: 运行数据清洗脚本")
    else:
        print("\n⚠️  数据下载失败")
        print("请检查Kaggle API配置或网络连接")

if __name__ == '__main__':
    main()

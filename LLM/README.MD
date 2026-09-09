Запуск ноутбука из папки LLM:
cd src\main\java\org\example\LLM
jupyter notebook

Отдельное окружение из environment.yml (по желанию):
conda env create -f environment.yml
conda activate llm

run:
python src\main\java\org\example\LLM\task1\main.py
or
cd src\main\java\org\example\LLM\task1
python main.py

If you want the project’s conda env:
cd src\main\java\org\example\LLM
conda env create -f environment.yml   # once
conda activate llm
python task1\main.py
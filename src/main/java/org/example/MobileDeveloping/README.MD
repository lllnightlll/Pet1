cd src\main\java\org\example\MobileDeveloping\task0\my_pwa_first
docker build -t my-pwa-lt .
docker run --rm -p 5500:5500 my-pwa-lt
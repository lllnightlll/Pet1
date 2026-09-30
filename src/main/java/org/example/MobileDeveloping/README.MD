cd src\main\java\org\example\MobileDeveloping\task0
docker build -f shop-pwa/Dockerfile -t my-pwa-lt .
docker run --rm -p 5500:5500 my-pwa-lt

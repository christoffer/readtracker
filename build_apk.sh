#!/bin/bash

printf "Store pass: "
read storepass
printf "Key pass: "
read keypass

mvn release:prepare &&
mvn release:perform -Dsign.storepass=$storepass -Dsign.keypass=$keypass &&
mvn release:clean

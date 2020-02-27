# 1DV701_pset2
Simple java HTTP server

## compiling
```
javac ./HTTPServer/HTTPServer.java
```

## running
```
java -cp . HTTPServer.HTTPServer 5000 public/
```

curl PUT request: update “/content/test1.png” resource
```
curl http://192.168.56.101:5000/content/test1.png --upload-file test1.png
```

curl POST request
```
curl -F img=@tony.png http://192.168.56.101:5000/content
```

see uploaded content by navigating to /content in browser


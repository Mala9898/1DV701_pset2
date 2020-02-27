# 1DV701_pset2
Simple java HTTP server

Instructions assume that you are currently in the folder where this readme is located.

## Compiling
```
javac ./HTTPServer/HTTPServer.java
```

## Running
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

To view uploaded content, navigate to the "/content" page in a browser.


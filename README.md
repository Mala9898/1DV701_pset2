# 1DV701_pset2
Simple java HTTP server

Instructions assume that you are currently in the folder where this readme is located.

Supported file types for PUT and POST: {.png}

A PUT request takes a {.png} file directly.

A POST request will specifically require the Content-Type: multipart/form-data, the file itself still needs to be a {.png}

Anything else will send a 415 Unsupported Media to your client!

Do note that the index.html file is modified from the original, the testa2.py script will show an error regarding the index page not being the same as the local copy. This is normal!


## Compiling
```
javac ./HTTPServer/HTTPServer.java
```

## Running
```
java -cp . HTTPServer.HTTPServer 5000 public/
```
## PUT and POST
curl PUT request: update “/content/test1.png” resource
```
curl http://192.168.56.101:5000/content/test1.png --upload-file test1.png
```

curl POST request
```
curl -F img=@tony.png http://192.168.56.101:5000/content
```

---

To view uploaded content, navigate to the "/content" page in a browser.


<div align="center">
  <img align="middle" src="https://github.com/I-Al-Istannen/Doctor/blob/master/media/Logo-Alpha.png?raw=true" height="200" width="200">
  <h1>Doc-Tor</h1>
</div>

Did you ever wish to tell somebody on discord to just read the (Java-)docs
while not being rude? Did you ever wish to point out some cool Java method or
class or highlight specific parts of the documentation?  
Then Doctor is for you :)

Doctor is best explained by a short demo:

https://user-images.githubusercontent.com/20284688/127779847-51c0b890-4ac8-4ddf-ac99-5054e40315bb.mp4

## Running Doctor
The Doctor jar file can be build using `mvn package` and just takes a single
argument:
```
java -jar Doctor.jar <path to config file>
```

Most of the magic happens in the config file. You can see an example in
[`src/main/resources/config.toml`](src/main/resources/config.toml).

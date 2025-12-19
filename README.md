# Javaish 1.0.2

**Javaish** is a Java-based navigation tool for plotting player ship movement in *Uncharted Waters Online*.  
It provides functionality similar to GVONavish, rebuilt from the ground up using Java for cross-platform compatibility.

> ⚠️ **Work in Progress**  
> This is an early release. The codebase is still evolving and may contain bugs or rough edges.

---

## Preview

<img width="1593" height="624" alt="Javaish preview" src="https://github.com/user-attachments/assets/7002bc18-11c7-4481-a840-1c555a9e01c9" />

---

## Features

- Supports custom maps (4096 × 2048)
- Compatible with **Windows** and **Linux**

---

## Requirements

- Java (JRE or JDK) installed
- Game settings configured to run either in a window or at your native resolution
- No graphical scaling active (the coordinate pixels need to be precise)

---

## Using the Application

1. Download the tagged release: [javaish-1.0.2.zip](https://github.com/Alexgopen/Javaish/releases/download/1.0.2/javaish-1.0.2.zip)
2. Extract the archive
3. Launch the application. **Windows:** `windows_launcher-1.0.2.bat` **Linux:** `linux_launcher-1.0.2.sh`
4. Sail around with **Surveying** active and coordinates visible on screen

---

## Using a Custom Map

- Replace the `map.png` file with your own image  
  *(Must be 4096 × 2048)*

---

## Building from Source

1. Check out the `release/1.0.2` branch from this repository
2. Build with Maven:
   `mvn clean install`
3. The compiled application will be located at:
   `/dist/javaish-1.0.2/`

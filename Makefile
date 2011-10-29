all : JavaBoy.class

run : JavaBoy.Class
	appletviewer javaboy.htm 	

JavaBoy.class : JavaBoy.java
	javac -Xlint:unchecked -Xlint:deprecation -O JavaBoy.java 

clean :
	rm -rf *.class

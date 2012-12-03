# Makefile is used to build the Simpella client 
# See README for more details
# Dept. of Computer Science, University at Buffalo
# Project - 2, CSE-589
# Authors: Sharath Chandrashekhara, Sanket Kulkarni
# 2012

FLAGS = -d
JAVAC = javac

CLASSES = src/*.java

OUTPUTDIR = bin

default: all
	
all:
	@echo "Compiling files..." 	
	@$(JAVAC) $(FLAGS) $(OUTPUTDIR) $(CLASSES)
	@echo "Done" 	

clean:
	@rm -rf bin/*
	@echo "Cleaned up the work space" 	

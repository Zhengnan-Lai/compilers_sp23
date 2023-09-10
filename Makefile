SRC = src/main/java/zc246_zl345_co232_mw756
ARG = t

.PHONY: all benchmark link clean

default: build

build:
	make clean
	make lexer
	make eta
	make rho
	gradle build

test_gen:
	python eth_gen.py

lexer:
	rm -f $(SRC)/lex/YyLex.java
	jflex -d $(SRC)/lex $(SRC)/YyLex.flex

zip:
	make clean
	zip -r etac.zip deps/ src/ test/ etac etac-build build.gradle settings.gradle \
		README.md Makefile ethScript benchmarks/ -x src/main/java/zc246_zl345_co232_mw756/T.java \
		src/main/java/zc246_zl345_co232_mw756/CFGTest.java \
		src/main/java/zc246_zl345_co232_mw756/RegisterAllocationTest.java \

clean:
	rm -f etac.zip
	rm -f $(SRC)/rho/parser.java
	rm -f $(SRC)/rho/sym.java
	rm -f $(SRC)/eta/parser.java
	rm -f $(SRC)/eta/sym.java
	rm -f $(SRC)/lex/YyLex.java
	gradle clean

sed:
	sed -i -e 's/\r$$//' etac-build
	sed -i -e 's/\r$$//' etac
	sed -i -e 's/\r$$//' link
	sed -i -e 's/\r$$//' time

link:
	./link out $(ARG).s && ./time ./out && rm -f out

log:
	git log > log.log

rho:
	rm -f $(SRC)/rho/parser.java
	rm -f $(SRC)/rho/sym.java
	java -jar deps/libs/main/java_cup.jar -destdir $(SRC)/rho $(SRC)/rho/rho.cup

eta:
	rm -f $(SRC)/eta/parser.java
	rm -f $(SRC)/eta/sym.java
	java -jar deps/libs/main/java_cup.jar -destdir $(SRC)/eta $(SRC)/eta/eta.cup

check:
	python checksol.py

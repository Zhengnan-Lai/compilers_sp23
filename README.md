# compilers-sp23
CS 4120: Compilers, Spring 2023

## Group Members
Chuhan Ouyang (co232) <br>
Michael Wei (mw756) <br>
Thomas Cui (zc246) <br>
Santiago Lai (zl345)

## Description
This compiler will be implemented in Java and use the Gradle build system.

### How to Use the Test Harness
Run the following commands in bash
1. `docker run -it -v <absolute path to "shared">:/home/student/shared charlessherk/cs4120-vm`
2. `cd shared`
3. `./etac-build`
4. `./etac  [options] <source files>`

### Known Issues
1. Error: `/usr/bin/env: 'bash\r': No such file or directory` <br>
Resolve with `sed -i -e 's/\r$//' scriptName`, then run `scriptName`

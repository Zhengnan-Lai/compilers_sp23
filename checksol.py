import os


def main(path):
    for _, dirs, _ in os.walk(os.getcwd() + "\\test" + path):
        for d in dirs:
            check(path + "\\" + d)


def check(path):
    d = os.listdir(os.getcwd() + "\\test" + path)
    for f in d:
        if f.endswith(".eta") or f.endswith(".rh"):
            name = f.split(".")[0]
            if name+".lexedsol" not in d \
                    and name+".parsedsol" not in d \
                    and name+".typedsol" not in d \
                    and name+".irsol.nml" not in d \
                    and name+".ssol.nml" not in d:
                print(path + os.sep + f)


if __name__ == "__main__":
    main("")

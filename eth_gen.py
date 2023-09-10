import os


def add(path, lex, parse, type, ir, asm):
    dir_list = os.listdir(os.getcwd() + "\\test" + path)
    for f in dir_list:
        try:
            name = f[:f.rindex(".")]
            if f.endswith(".lexedsol"):
                append(lex, dir_list, name)
            elif f.endswith(".parsedsol"):
                append(parse, dir_list, name)
            elif f.endswith(".typedsol"):
                append(type, dir_list, name)
            elif f.endswith(".irsol.nml"):
                name = name[:name.rindex(".")]
                append(ir, dir_list, name)
            elif f.endswith(".ssol.nml"):
                name = name[:name.rindex(".")]
                append(asm, dir_list, name)
        except ValueError:
            pass


def append(lst, dir_list, name):
    if name + ".eta" in dir_list:
        lst.append(name + ".eta")
    elif name + ".eti" in dir_list:
        lst.append(name + ".eti")
    elif name + ".rh" in dir_list:
        lst.append(name + ".rh")
    elif name + ".ri" in dir_list:
        lst.append(name + ".ri")


def write(path):
    lex = []
    parse = []
    type = []
    ir = []
    asm = []
    add(path, lex, parse, type, ir, asm)
    fname = "ethScript\\eth" + path.replace("\\", "_")
    eth = open(fname, "w")
    p = "" if path == "" else path[1:]
    p = p.replace("\\", "/")
    s = "eth -testpath " + "test/" + p + ' ' + fname.replace("\\", "/") + "\n"
    eth.write("// " + s + "\n")
    eth.write("build\n\n")
    eth.write('etac ("Test --help") "--help" {\n}\n\n')

    eth.write('etac ("Test --lex") "--lex" {\n')
    for f in lex:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write('etac ("Test --parse") "--parse" {\n')
    for f in parse:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write('etac ("Test --typecheck") "-libpath test/lib --typecheck" {\n')
    for f in type:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write('etac ("Test --irgen") "-libpath test/lib --irgen" {\n')
    for f in ir:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write('etac ("Test -O --irgen") "-libpath test/lib --irgen -O" {\n')
    for f in ir:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -target linux") "-libpath test/lib -target linux" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -O -target linux") "-libpath test/lib -target linux -O" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Oreg -target linux") "-libpath test/lib -target linux -Oreg" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Ocf -target linux") "-libpath test/lib -target linux -Ocf" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Odce -target linux") "-libpath test/lib -target linux -Odce" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Ocp -target linux") "-libpath test/lib -target linux -Ocp" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Ocopy -target linux") "-libpath test/lib -target linux -Ocopy" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')

    eth.write(
        'etac ("Test -Ovn -target linux") "-libpath test/lib -target linux -Ovn" {\n')
    for f in asm:
        eth.write(f'\t{f};\n')
    eth.write('}\n\n')
    eth.close()


def main(path):
    for _, dirs, _ in os.walk(os.getcwd() + "\\test" + path):
        for d in dirs:
            write(path + "\\" + d)


if __name__ == "__main__":
    main("")
    print("Done")

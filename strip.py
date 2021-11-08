with open("2_000_000_000.txt", "rb") as f:
    with open("2B_stripped.txt", "w") as stripped:
        for line in f:
            try:
                txt = line.decode('ascii')
                if len(txt) > 3:
                    stripped.write(txt)
            except Exception as e:
                print(e)



if __name__ == '__main__':
    with open("cracked_passwords_2B.txt", "r") as f_in:
        with open("formatted_passwords_2B.txt", "w") as f_out:
            for line in f_in:
                username, password = line.strip().split(':')
                f_out.write(f"'{username}': '{password}',\n")

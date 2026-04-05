import sys
import re
import os

def update_file(fname, search_text, replace_text):
    if not os.path.exists(fname):
        print(f"File not found: {fname}")
        return
    with open(fname, 'r', encoding='utf-8') as f:
        c = f.read()
    if replace_text not in c:
        c = c.replace(search_text, replace_text)
        with open(fname, 'w', encoding='utf-8') as f:
            f.write(c)
        print(f"Updated {fname}")
    else:
        print(f"Already updated {fname}")

fname1 = 'src/main/java/com/fintech/ewallet/identity/infrastructure/persistence/UserJpaEntity.java'
search1 = '    private String email;\n'
replace1 = '''    private String email;

    @Column(name = "english_full_name", length = 100)
    private String englishFullName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth", length = 20)
    private String dateOfBirth;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
'''
update_file(fname1, search1, replace1)

fname2 = 'src/main/java/com/fintech/ewallet/identity/domain/User.java'
search2a = '    private String email; // Optional\n'
replace2a = '''    private String email; // Optional
    private String englishFullName;
    private String gender;
    private String dateOfBirth;
    private String idNumber;
    private String maritalStatus;
'''
update_file(fname2, search2a, replace2a)

search2b = '''String passwordHash, String email,
            String language, String referralCode'''
replace2b = '''String passwordHash, String email,
            String language, String referralCode,
            String englishFullName, String gender, String dateOfBirth, String idNumber, String maritalStatus'''
update_file(fname2, search2b, replace2b)

search2c = '''        user.email = email;'''
replace2c = '''        user.email = email;
        user.englishFullName = englishFullName;
        user.gender = gender;
        user.dateOfBirth = dateOfBirth;
        user.idNumber = idNumber;
        user.maritalStatus = maritalStatus;'''
update_file(fname2, search2c, replace2c)

with open(fname2, 'r', encoding='utf-8') as f:
    content = f.read()
if 'getEnglishFullName' not in content:
    gs = '''
    public String getEnglishFullName() { return englishFullName; }
    public void setEnglishFullName(String englishFullName) { this.englishFullName = englishFullName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
'''
    pos = content.rfind('}')
    content = content[:pos] + gs + '}\n'
    with open(fname2, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated getters in {fname2}")

fname3 = 'src/main/java/com/fintech/ewallet/identity/application/dto/RegisterRequest.java'
search3 = '''String referralCode // Optional'''
replace3 = '''String referralCode, // Optional

        String englishFullName,
        String gender,
        String dateOfBirth,
        String idNumber,
        String maritalStatus'''
update_file(fname3, search3, replace3)

fname4 = 'src/main/java/com/fintech/ewallet/identity/application/RegisterUserUseCase.java'
search4 = '''request.email(),
                request.language(),
                referralCode'''
replace4 = '''request.email(),
                request.language(),
                referralCode,
                request.englishFullName(),
                request.gender(),
                request.dateOfBirth(),
                request.idNumber(),
                request.maritalStatus()'''
update_file(fname4, search4, replace4)

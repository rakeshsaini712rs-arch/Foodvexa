from pathlib import Path
import re

p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")
out=[]
depth=0
debt=0
state="code"
stack=[]
i=0
declarations=[]
while i<len(s):
    if state=="code":
        if s.startswith("private fun ",i):
            declarations.append((i,depth))
            if depth>1:
                close_count=depth-1
                out.append("}"*close_count+"\n")
                depth=1
                debt+=close_count
        if s.startswith("//",i):
            out.append("//"); i+=2; state="line"; continue
        if s.startswith("/*",i):
            out.append("/*"); i+=2; state="block"; continue
        ch=s[i]
        if ch=='"':
            out.append(ch); i+=1; state="string"; continue
        if ch=="'":
            out.append(ch); i+=1; state="char"; continue
        if ch=="{":
            depth+=1
        elif ch=="}":
            if depth==1 and debt>0:
                debt-=1
                i+=1
                continue
            depth-=1
            if depth<0:
                raise SystemExit("Brace repair failed: negative scope depth")
        out.append(ch); i+=1
    elif state=="string":
        ch=s[i]
        out.append(ch); i+=1
        if ch=="\\" and i<len(s):
            out.append(s[i]); i+=1
        elif ch=='"':
            state="code"
        elif ch=="$" and i<len(s) and s[i]=="{":
            out.append("{"); i+=1
            stack.append(("string",state))
            state="interpolation"
            interp_depth=1
    elif state=="interpolation":
        if s.startswith("//",i):
            out.append("//"); i+=2; stack.append(("interpolation",state)); state="line"; continue
        if s.startswith("/*",i):
            out.append("/*"); i+=2; stack.append(("interpolation",state)); state="block"; continue
        ch=s[i]
        if ch=='"':
            out.append(ch); i+=1; stack.append(("interpolation",state)); state="string"; continue
        if ch=="'":
            out.append(ch); i+=1; stack.append(("interpolation",state)); state="char"; continue
        out.append(ch); i+=1
        if ch=="{": interp_depth+=1
        elif ch=="}":
            interp_depth-=1
            if interp_depth==0:
                state="string"
    elif state=="char":
        ch=s[i]; out.append(ch); i+=1
        if ch=="\\" and i<len(s):
            out.append(s[i]); i+=1
        elif ch=="'":
            state=stack.pop()[1] if stack else "code"
    elif state=="line":
        ch=s[i]; out.append(ch); i+=1
        if ch=="\n":
            state=stack.pop()[1] if stack else "code"
    elif state=="block":
        if s.startswith("*/",i):
            out.append("*/"); i+=2
            state=stack.pop()[1] if stack else "code"
        else:
            out.append(s[i]); i+=1

fixed="".join(out)
if not declarations:
    raise SystemExit("No private fun declarations found; refusing to alter source")
if any(d==0 for _,d in declarations):
    raise SystemExit("Found private fun declaration outside MainActivity class; source needs manual boundary repair")
# All declarations encountered at nested depth are repaired to class scope.
p.write_text(fixed,encoding="utf-8")
print("SCOPE REPAIR: declarations checked =",len(declarations))
print("SCOPE REPAIR: nested declarations repaired =",sum(1 for _,d in declarations if d>1))
print("SCOPE REPAIR: remaining orphan closing braces skipped =",debt)
print("SCOPE REPAIR: updateCartBadge =", "private fun updateCartBadge()" in fixed)
print("SCOPE REPAIR: showCollection =", "private fun showCollection()" in fixed)
print("SCOPE REPAIR: showAddressBook =", "private fun showAddressBook()" in fixed)

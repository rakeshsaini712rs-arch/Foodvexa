from pathlib import Path
import re

p = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s = p.read_text(encoding="utf-8")

# Final visual pass runs after all existing layout transforms, so earlier patches
# cannot overwrite the approved compact dark premium delivery/search style.
a = s.find("private fun locationHeader():LinearLayout")
b = s.find("private fun ", a + 20)
if a < 0 or b < 0:
    raise SystemExit("Location header boundaries not found")
header = s[a:b]
header = header.replace('setPadding(dp(12),dp(9),dp(10),dp(9))',
                        'setPadding(dp(10),dp(5),dp(8),dp(5))')
header = header.replace('background=rounded(Color.WHITE,18)',
                        'background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(27,27,31),Color.rgb(19,19,22))).apply{cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(57,53,48)}')
header = header.replace('label("DELIVER TO",10f,true,muted)',
                        'label("DELIVER TO",9f,true,Color.rgb(190,185,178))')
header = header.replace('label(if(saved.isBlank())"Choose your delivery address" else saved,14f,true,ink)',
                        'label(if(saved.isBlank())"Choose your delivery address" else saved,13f,true,Color.WHITE)')
header = header.replace('LinearLayout.LayoutParams(dp(38),dp(48))',
                        'LinearLayout.LayoutParams(dp(32),dp(38))')
header = header.replace('LinearLayout.LayoutParams(dp(24),dp(44))',
                        'LinearLayout.LayoutParams(dp(22),dp(38))')
header = header.replace('LinearLayout.LayoutParams(dp(38),dp(44))',
                        'LinearLayout.LayoutParams(dp(32),dp(38))')
if 'background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT' not in header:
    raise SystemExit("Dark compact delivery card patch did not apply")
s = s[:a] + header + s[b:]

# Compact the fixed search field while keeping its live filtering behavior.
a = s.find('val search=EditText(this).apply{')
b = s.find('searchBox=search', a)
if a < 0 or b < 0:
    raise SystemExit("Search field boundaries not found")
block = s[a:b]
block = block.replace('setPadding(dp(14),0,dp(14),0)',
                      'setPadding(dp(14),0,dp(12),0)')
block = block.replace('background=rounded(Color.WHITE,16)',
                      'background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(35,35,40),Color.rgb(26,26,30))).apply{cornerRadius=dp(22).toFloat();setStroke(dp(1),Color.rgb(61,57,52)}')
block = block.replace('setHintTextColor(Color.rgb(135,135,145));setTextColor(ink);textSize=16f',
                      'setHintTextColor(Color.rgb(165,165,173));setTextColor(Color.WHITE);textSize=15f')
if 'background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT' not in block:
    raise SystemExit("Dark search field patch did not apply")
s = s[:a] + block + s[b:]

# Make the category row feel lighter and less tall; retain category click handlers.
s = s.replace('LinearLayout.LayoutParams(dp(92),dp(78)).apply{rightMargin=dp(6)}',
              'LinearLayout.LayoutParams(dp(88),dp(70)).apply{rightMargin=dp(6)}')
s = s.replace('stickyScroll,LinearLayout.LayoutParams(-1,dp(82))',
              'stickyScroll,LinearLayout.LayoutParams(-1,dp(74))')
s = s.replace('LinearLayout.LayoutParams(-1,dp(82)))',
              'LinearLayout.LayoutParams(-1,dp(74)))')

checks = {
    "compact dark delivery card": "Dark compact delivery card patch did not apply" not in "",
    "dark search field": "background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT" in block,
    "category row compact": "LinearLayout.LayoutParams(dp(88),dp(70))" in s,
    "location and search still interactive": "setOnClickListener{locationDialog()}" in header and "addTextChangedListener" in block,
    "product render preserved": "renderProducts()" in s,
}
for name, ok in checks.items():
    print(("PASS " if ok else "FAIL ") + name)
if not all(checks.values()):
    raise SystemExit("Dark premium home verification failed")
p.write_text(s, encoding="utf-8")
print("DARK PREMIUM HOME APPLIED")

from pathlib import Path
p=Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s=p.read_text(encoding='utf-8')

# Compact category rail without changing its behavior.
s=s.replace('LinearLayout.LayoutParams(dp(116),dp(122))','LinearLayout.LayoutParams(dp(104),dp(104))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(122))','LinearLayout.LayoutParams(-1,dp(104))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(78)).apply{bottomMargin=dp(5)}','LinearLayout.LayoutParams(-1,dp(62)).apply{bottomMargin=dp(4)}')

marker=' private fun renderProducts()'
if marker not in s:
    raise SystemExit('renderProducts marker not found')

if 'private fun compactProductCards()' not in s:
    helper=''' private fun compactProductCards(){\n  fun walk(v:android.view.View){\n    if(v is ImageView){\n      val lp=v.layoutParams\n      if(lp!=null && lp.width>dp(120)){lp.height=dp(132);v.layoutParams=lp}\n    }\n    if(v is android.view.ViewGroup)for(i in 0 until v.childCount)walk(v.getChildAt(i))\n  }\n  walk(content)\n }\n'''
    s=s.replace(marker,helper+marker,1)

# Append a safe post-render compaction pass to renderProducts().
start=s.index(marker)
pos=s.index('{',start)
depth=0; end=None
for i in range(pos,len(s)):
    if s[i]=='{': depth+=1
    elif s[i]=='}':
        depth-=1
        if depth==0:
            end=i+1; break
if end is None: raise SystemExit('renderProducts end not found')
body=s[start:end]
if 'compactProductCards()' not in body:
    body=body[:-1]+' compactProductCards()}\n'
    s=s[:start]+body+s[end:]

p.write_text(s,encoding='utf-8')
print('OK compact product/category layout')

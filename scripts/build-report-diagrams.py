"""Render source-controlled UML views of the implemented system (no network)."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/diagrams'
OUT.mkdir(exist_ok=True)
FONT = 'C:/Windows/Fonts/simsun.ttc'
INK = '#222222'

def start(w=1800, h=1100):
    global im, draw
    im = Image.new('RGB', (w,h), 'white'); draw=ImageDraw.Draw(im)

def text(x,y,s,size=30,anchor='mm'):
    draw.multiline_text((x,y),s,font=ImageFont.truetype(FONT,size),fill=INK,anchor=anchor,align='center',spacing=10)

def line(points,dashed=False):
    if not dashed: draw.line(points,fill=INK,width=3); return
    for (x,y),(xx,yy) in zip(points,points[1:]):
        n=max(1,int(math.hypot(xx-x,yy-y)/14))
        for i in range(0,n,2): draw.line([(x+(xx-x)*i/n,y+(yy-y)*i/n),(x+(xx-x)*min(i+1,n)/n,y+(yy-y)*min(i+1,n)/n)],fill=INK,width=2)

def arrow(points,label='',dashed=False,dy=-28):
    line(points,dashed)
    x,y=points[-1];a,b=points[-2];ang=math.atan2(y-b,x-a)
    line([(x-17*math.cos(ang-.45),y-17*math.sin(ang-.45)),(x,y),(x-17*math.cos(ang+.45),y-17*math.sin(ang+.45))])
    if label:
        a,b=points[0]; text((a+x)/2,(b+y)/2+dy,label,26)

def box(x,y,w,h,label,rounded=False):
    fn=draw.rounded_rectangle if rounded else draw.rectangle
    kw={'radius':20} if rounded else {}
    fn((x,y,x+w,y+h),fill='white',outline=INK,width=3,**kw);text(x+w/2,y+h/2,label)

def save(name): im.save(OUT / (name+'.png'),dpi=(300,300))

def actor(x,y,label):
    draw.ellipse((x-24,y-70,x+24,y-22),outline=INK,width=3)
    line([(x,y-22),(x,y+48)]);line([(x-42,y),(x+42,y)]);line([(x-34,y+92),(x,y+48),(x+34,y+92)])
    text(x,y+125,label,30)

start(1800,1180)
draw.rectangle((340,65,1480,1110),outline=INK,width=3);text(910,105,'宠物管家系统：角色与用例',36)
actor(140,325,'宠物主人');actor(1660,310,'商户');actor(1660,815,'管理员')
cases=[(650,265,'维护宠物档案'),(650,430,'记录照护与提醒'),(650,595,'查询医院与购物'),(650,760,'使用六项 AI 功能'),(650,970,'维护资料与退出'),(1200,265,'维护店铺商品'),(1200,430,'处理本店订单'),(1200,595,'查询租金账单'),(1200,785,'查看平台统计'),(1200,950,'管理用户 / 宠物\n审核商户')]
for x,y,s in cases:
    draw.ellipse((x-220,y-55,x+220,y+55),fill='white',outline=INK,width=3);text(x,y,s,30)
for y in [265,430,595,760,970]: line([(200,325),(430,y)])
for y in [265,430,595]: line([(1600,310),(1420,y)])
for y in [785,950]: line([(1600,815),(1420,y)])
line([(1600,310),(1510,665),(1510,1060),(870,1060),(870,970)])
line([(1600,815),(1535,1060),(870,1060)])
save('use-cases')

start(1800,1090)
box(80,90,480,150,'«component»\nVue 3 页面 / 路由 / Axios')
box(660,90,480,150,'«component»\nSecurity / JWT / CSRF / RBAC')
box(1240,90,480,150,'«component»\nREST Controller / 请求校验')
arrow([(560,165),(660,165)],'HTTP',dy=-95);arrow([(1140,165),(1240,165)],'认证身份',dy=-95)
box(1240,380,480,160,'«component»\nService 业务 / 事务 / 归属校验');arrow([(1480,240),(1480,380)],'调用',dy=0)
box(80,720,480,160,'«component»\nMyBatis Mapper / JPA Repository')
box(660,720,480,160,'«component»\nAssistantService / AI Gateway')
box(1240,720,480,160,'«component»\nFileUtils / 上传文件存储')
arrow([(1380,540),(1380,615),(320,615),(320,720)])
arrow([(1480,540),(1480,650),(900,650),(900,720)])
arrow([(1580,540),(1580,720)])
box(80,940,480,100,'«database» MySQL / demo H2');arrow([(320,880),(320,940)])
box(660,940,480,100,'项目资料 / 兼容模型 API');arrow([(900,880),(900,940)])
box(1240,940,480,100,'受控 uploads 目录');arrow([(1480,880),(1480,940)])
save('components')

start(1800,1280)
def cls(x,y,title,fields,w=450,h=230):
    box(x,y,w,h,'');line([(x,y+65),(x+w,y+65)]);text(x+w/2,y+33,title,32)
    draw.multiline_text((x+18,y+86),'\n'.join(f if f.startswith('+ ') else '- '+f for f in fields),font=ImageFont.truetype(FONT,27),fill=INK,spacing=9)
cls(50,60,'User 用户',['id: Long','username: String','role: String','passwordHash: String'])
cls(675,60,'Pet 宠物',['id: Long','owner: User','species: String','birthDate: LocalDate'])
cls(1300,60,'HealthRecord',['id: Long','pet: Pet','recordType: String','recordDate: LocalDate'])
cls(50,470,'Product 商品',['id: Long','merchant: User','price: Double','stock: Integer'])
cls(675,470,'Order 订单',['id: Long','user: User','status: String','totalAmount: Double'])
cls(1300,470,'OrderItem 明细',['order: Order','product: Product','productPrice: Double','quantity: Integer'])
cls(50,900,'CartItem 购物车项',['userId: Long','product: Product','quantity: Integer'])
cls(675,900,'Reminder 提醒',['pet: Pet','user: User','title: String','isDismissed: Boolean'])
for a,b,left,right in [((500,175),(675,175),'1','0..*'),((1125,175),(1300,175),'1','0..*'),((500,585),(675,585),'0..*','1'),((1125,585),(1300,585),'1','1..*')]:
    # Product/Order intentionally has no direct association; relation handled below.
    if a==(500,585): continue
    line([a,b]);text(a[0]+35,a[1]-30,left,25);text(b[0]-45,b[1]-30,right,25)
line([(275,290),(275,470)]);text(325,320,'0..1',25);text(330,440,'0..*',25);text(170,380,'merchant',25)
line([(500,250),(575,250),(575,375),(900,375),(900,470)]);text(540,230,'1',25);text(940,440,'0..*',25);text(730,350,'buyer',25)
line([(500,670),(580,670),(580,790),(1525,790),(1525,700)]);text(540,642,'0..1',25);text(1565,750,'0..*',25)
line([(275,700),(275,900)]);text(305,740,'1',25);text(330,865,'0..*',25)
line([(500,130),(610,130),(610,850),(70,850),(70,900)]);text(570,100,'1',25);text(105,875,'0..*',25)
line([(900,290),(1175,290),(1175,835),(900,835),(900,900)]);text(1205,315,'1',25);text(940,865,'0..*',25)
text(1375,1020,'仅列主要属性与关联\n未绘全部照护与分析历史类\n订单明细保存成交时名称与价格',29)
save('domain-classes')

start(1800,1220)
xs=[170,525,880,1235,1610];names=['用户 / Vue','AuthController','AuthService','数据库 / BCrypt','JWT / Security']
for x,s in zip(xs,names):
    box(x-150,40,300,95,s);line([(x,135),(x,1170)],True)
events=[(0,1,210,'获取 CSRF / 提交账号密码',False),(1,2,320,'login(request)',False),(2,3,430,'查用户 / matches(password, hash)',False),(3,2,540,'用户与校验结果',True),(2,1,650,'认证成功 / 用户角色',True),(1,4,760,'签发 JWT，写 HttpOnly Cookie',False),(1,0,870,'登录结果；跳转本角色工作台',True),(0,4,980,'后续请求：Cookie + CSRF 头',False),(4,0,1090,'验签 / 撤销检查 / 权限判定',True)]
for a,b,y,s,d in events:arrow([(xs[a],y),(xs[b],y)],s,d)
save('login-sequence')

start(1800,940)
draw.ellipse((75,315,115,355),fill=INK);arrow([(115,335),(260,335)]);text(190,220,'下单 / 扣库存',26)
box(260,260,300,150,'pending\n待付款',True);box(740,260,300,150,'paid\n已付款状态',True);box(1230,260,300,150,'shipped\n已发货',True)
box(740,650,300,150,'cancelled\n已取消',True);box(1230,650,300,150,'delivered\n已完成',True)
arrow([(560,335),(740,335)],'确认状态',dy=-55);arrow([(1040,335),(1230,335)],'发货',dy=-55)
arrow([(1380,410),(1380,650)]);text(1460,530,'完成',26)
arrow([(410,410),(410,725),(740,725)],'取消 / 恢复库存',dy=-35)
arrow([(890,410),(890,650)]);text(1010,530,'取消\n恢复库存',26)
text(900,130,'订单状态机（ShopService 的允许迁移）',37)
text(880,880,'“已付款”为人工业务状态；未接入支付网关。重复请求不重复回补库存。',28)
save('order-states')

start(1800,1410)
draw.ellipse((880,35,920,75),fill=INK);arrow([(900,75),(900,120)])
box(650,120,500,100,'接收问题 / 长度与字符校验',True);arrow([(900,220),(900,275)])
def diamond(x,y,w,h,s):
    draw.polygon([(x,y-h/2),(x+w/2,y),(x,y+h/2),(x-w/2,y)],fill='white',outline=INK,width=3);text(x,y,s,28)
diamond(900,350,470,150,'模型网关可用？')
box(80,500,550,120,'词法检索 Top 3',True)
box(1120,500,600,120,'资料向量缓存 / 问题向量\n余弦相似度 > 0.15，Top 3',True)
arrow([(665,350),(355,350),(355,500)],'否',dy=-20);arrow([(1135,350),(1420,350),(1420,500)],'是',dy=-20)
box(1120,710,600,120,'固定 System 规则 + 引用资料\n+ User 问题 → 对话接口',True);arrow([(1420,620),(1420,710)])
diamond(1420,960,490,160,'响应与引用 ID 合法？');arrow([(1420,830),(1420,880)])
box(80,1030,550,130,'返回原文与真实出处\nstatus = retrieval_only',True)
box(1120,1170,600,115,'返回回答与出处\nstatus = answered',True)
arrow([(355,620),(355,1030)]);text(455,800,'[有资料]',26)
box(650,700,430,130,'返回 no_sources\n资料中没有相关内容',True)
arrow([(630,560),(870,560),(870,700)]);text(780,535,'[无资料]',26)
arrow([(1120,560),(1080,560),(1080,750)]);text(990,535,'[无资料]',26)
arrow([(865,830),(865,1250),(670,1250),(670,1340),(875,1340)])
arrow([(1175,960),(355,960),(355,1030)]);text(780,925,'否 / 异常：词法检索后降级',26)
arrow([(1420,1040),(1420,1170)]);text(1480,1110,'是',26)
arrow([(355,1160),(355,1340),(900,1340)])
arrow([(1420,1285),(1420,1340),(900,1340)])
draw.ellipse((875,1315,925,1365),outline=INK,width=3);draw.ellipse((885,1325,915,1355),fill=INK)
save('rag-activity')
start(1800,1100)
text(900,55,'资料问答模块的组件依赖',37)
box(80,150,460,145,'«component»\nVue 问答页面 / Axios')
box(670,150,460,145,'«component»\nSecurity / JWT / CSRF')
box(1260,150,460,145,'«component»\nAssistantController')
arrow([(540,223),(670,223)],dashed=True)
arrow([(1130,223),(1260,223)],dashed=True)
box(1260,455,460,150,'«component»\nAssistantService')
arrow([(1490,295),(1490,455)],dashed=True);text(1640,375,'ask(question)',26)
box(80,455,460,150,'«component»\nKnowledgeCatalog')
box(670,455,460,150,'«component»\nKnowledgeChunk 集合')
arrow([(540,530),(670,530)],'加载切块',dashed=True,dy=-40)
arrow([(1260,530),(1130,530)],'读取片段',dashed=True,dy=-40)
box(80,790,460,150,'«artifact»\nknowledge/project-guide.md')
box(670,790,460,150,'«component»\nAiGateway / OpenAiGateway')
box(1260,790,460,150,'«external service»\n兼容向量与对话 API')
arrow([(310,605),(310,790)],dashed=True)
arrow([(1380,605),(1380,700),(900,700),(900,790)],dashed=True)
arrow([(1130,865),(1260,865)],'SDK',dashed=True,dy=-40)
text(900,1020,'依赖视图：网关不可用时由 AssistantService 词法检索并降级，前端不持有 API Key。',28)
save('ai-components')

start(1800,1350)
text(900,45,'RAG 核心类与接口',37)
cls(50,115,'AssistantController',['service: AssistantService','+ ask(AskRequest)'],w=490,h=220)
cls(650,115,'AssistantService',['chunks: List<KnowledgeChunk>','gateway: AiGateway','cachedEmbeddings: List<List<Double>>','+ ask(String): AskResult'],w=580,h=290)
cls(1300,115,'«interface» AiGateway',['+ available(): boolean','+ embed(List<String>)','+ answer(question, context)'],w=450,h=245)
arrow([(540,225),(650,225)],dashed=True)
arrow([(1230,225),(1300,225)],dashed=True)
cls(1300,630,'OpenAiGateway',['client: OpenAIClient','chatModel: String','embeddingModel: String'],w=450,h=245)
line([(1525,630),(1525,385)],dashed=True)
draw.polygon([(1507,385),(1543,385),(1525,360)],fill='white',outline=INK,width=3)
text(1630,480,'实现',26)
cls(50,630,'KnowledgeCatalog',['+ load(): List<KnowledgeChunk>','+ parse(file, markdown)'],w=490,h=220)
cls(650,630,'«record» KnowledgeChunk',['id: String','title: String','text: String'],w=580,h=245)
arrow([(540,740),(650,740)],'创建',dashed=True,dy=-40)
line([(920,405),(920,630)]);text(955,440,'1',26);text(965,600,'0..*',26)
cls(50,1030,'«record» AskResult',['answer: String','sources: List<Source>','status: String'],w=490,h=245)
cls(650,1030,'«record» Source',['id: String','title: String','excerpt: String'],w=580,h=245)
line([(540,1155),(650,1155)]);text(560,1115,'1',25);text(615,1190,'0..*',25)
arrow([(810,405),(575,405),(575,940),(295,940),(295,1030)],dashed=True)
text(1510,1110,'仅列核心属性与操作\nAskResult / Source 为\nAssistantService 内部 record\n虚线表示依赖或接口实现',28)
save('rag-classes')

start(1800,1340)
text(900,40,'品种分析样例的正常提交与历史保存',37)
sx=[150,485,820,1155,1650]
for x,label in zip(sx,['用户 / Vue','Security','AiController','AiService','上传 / Repository']):
    box(x-135,100,270,105,label);line([(x,205),(x,1220)],True)
text(410,260,'填入样例或替换图片：仅更新表单，不发送请求',28)
arrow([(sx[0],350),(sx[1],350)],'点击提交：multipart + Cookie + CSRF',dy=-32)
arrow([(sx[1],465),(sx[2],465)],'认证 / 写请求校验通过',dy=-32)
arrow([(sx[2],580),(sx[4],580)],'查当前用户与 petId；校验宠物归属',dy=-32)
arrow([(sx[2],695),(sx[4],695)],'FileUtils.saveUpload(image)',dy=-32)
arrow([(sx[2],810),(sx[3],810)],'predictBreed(path)',dy=-32)
arrow([(sx[3],910),(sx[2],910)],'本地颜色启发式结果',dashed=True,dy=-32)
arrow([(sx[2],1005),(sx[3],1005)],'saveBreedHistory(...)',dy=-32)
arrow([(sx[3],1095),(sx[4],1095)],'保存关联用户、可选宠物与结果',dy=-32)
arrow([(sx[2],1190),(sx[0],1190)],'响应：结果 + history_id + image_url',dashed=True,dy=-32)
text(900,1290,'选取成功路径；身份、归属或图片格式失败时提前返回，不能继续分析和保存。',28)
save('ai-sample-sequence')

start(1800,1200)
text(900,50,'一次资料问答请求的结果状态',37)
draw.ellipse((90,155,130,195),fill=INK);arrow([(130,175),(300,175)])
box(300,115,530,120,'接收并校验问题',True)
box(1110,115,590,120,'invalid_question\nHTTP 400 / 输入不符合限制',True)
arrow([(830,175),(1110,175)],'[非法输入]',dy=-40)
box(300,385,530,120,'检索 / 可选生成 / 引用校验',True)
arrow([(565,235),(565,385)]);text(690,310,'[合法输入]',27)
box(90,795,460,150,'no_sources\n无相关资料',True)
box(675,795,460,150,'retrieval_only\n返回资料原文与出处',True)
box(1260,795,460,150,'answered\n生成回答及合法引用',True)
arrow([(420,505),(420,705),(320,705),(320,795)])
text(250,605,'[检索集合为空]',27)
arrow([(565,505),(565,740),(905,740),(905,795)])
text(900,620,'[有资料且网关不可用\n或服务异常 / 引用不合法]',27)
arrow([(720,505),(1490,505),(1490,795)])
text(1325,620,'[有资料、生成成功\n且引用 ID 校验通过]',27)
draw.ellipse((875,1050,925,1100),outline=INK,width=3);draw.ellipse((885,1060,915,1090),fill=INK)
for x in [320,905,1490]:arrow([(x,945),(x,1075),(900,1075)])
arrow([(1700,175),(1760,175),(1760,1075),(925,1075)])
text(900,1160,'状态表示本次接口返回结果；不是数据库状态迁移，也不能据 answered 推断语义正确。',28)
save('assistant-result-states')
print('Rendered 10 UML diagrams to',OUT)

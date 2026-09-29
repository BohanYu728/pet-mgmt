# UI 摄影素材来源

这些图片是页面氛围或商城分类的**示意照片**，不能当作用户上传的宠物照片或具体在售商品图片。商城有商品自身图片时，应优先显示商品图片。外部照片下载为本地静态资源，浏览器运行时不依赖第三方图片服务。

| 本地文件 | 画面与用途 | 摄影者、来源 | 授权 |
| --- | --- | --- | --- |
| `frontend/public/images/cat-lifestyle.jpg` | 暖色橘猫，生活场景 | 从项目原有 `src/main/resources/static/uploads/test_photos/ginger_cat_warm.jpg` 原样复制；原始摄影者和授权来源在项目中未注明 | **来源及再使用授权未核实**；仅沿用项目既有测试素材，公开发布前需确认权利 |
| `frontend/public/images/dog-lifestyle.jpg` | 秋天落叶林中站立的金毛犬；横幅或登录页氛围图 | Dirk Vorderstraße，[Wikimedia Commons: Golden retriever stehfoto.jpg](https://commons.wikimedia.org/wiki/File:Golden_retriever_stehfoto.jpg) | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) |
| `frontend/public/images/product-food.jpg` | 白碗中的狗粮；食品分类示意 | James Redekop，[Wikimedia Commons: Dog food in a bowl.jpg](https://commons.wikimedia.org/wiki/File:Dog_food_in_a_bowl.jpg) | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/) |
| `frontend/public/images/product-toy.jpg` | 蓝色骨形犬玩具；玩具分类示意，照片中的品牌不代表平台在售商品 | Blackey1234，[Wikimedia Commons: Blue dog bone toy.JPG](https://commons.wikimedia.org/wiki/File:Blue_dog_bone_toy.JPG) | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) |
| `frontend/public/images/product-care.jpg` | 宠物梳毛刷；护理用品分类示意 | ZooFari，[Wikimedia Commons: Dog brush.JPG](https://commons.wikimedia.org/wiki/File:Dog_brush.JPG) | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) |

四张 Commons 图片由对应文件页的 `Special:Redirect/file` 下载，宽度参数分别为 1600（犬、玩具、护理）与 1000（狗粮）；因此是 Commons 提供的缩放版本。没有进一步改动像素。页面 CSS 裁切不改变源文件。使用时应保留摄影者、文件页及授权链接；若另行改图，应按相应授权标明修改，ShareAlike 图片的改作还须遵循相同授权。

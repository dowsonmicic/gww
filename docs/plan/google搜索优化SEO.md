# GWW 塑料原料站 Google 搜索优化方案（SEO）

> 目标：让 Google 用户用"道达尔 TPV""TPU 弹性体""聚丙烯 PP 原料""汽车密封条塑料"等关键词，能搜到 GWW 的官网产品页。
> 适用站点：`website/`（Nuxt 4 SSR）+ `backend/server/`（Spring Boot）

---

## 一、现状体检

| 检查项 | 当前状态 | 严重程度 |
|--------|---------|---------|
| Google 能否爬取 | ❌ `nuxt.config.ts` 里全站 `noindex, nofollow` | 🔴 致命 |
| robots.txt | ❌ 无独立文件 | 🟡 缺 |
| sitemap.xml | ❌ 无 | 🟡 缺 |
| 产品页 title | ⚠️ 模板化 `"{category} {model}"`，缺公司名 | 🟠 需优化 |
| 产品页 description | ❌ 所有页面写死同一段 intro | 🔴 严重 |
| canonical URL | ❌ 无 | 🟡 缺 |
| hreflang（双语互指） | ❌ 无 | 🟠 重复内容风险 |
| Open Graph / Twitter Card | ❌ 无 | 🟡 缺 |
| schema.org 结构化数据 | ❌ 无 | 🟢 加分项 |
| SSR 完整 HTML | ✅ 已有 | ✅ 优势 |
| URL 结构 | ✅ `/zh/products/{id}` 清晰 | ✅ 优势 |
| SSL | ❌ 未部署 | 🔴 排名因子 |
| 加载速度 | ⚠️ SSR 基础好，但 admin CSS 354KB 过大 | 🟡 需优化 |

---

## 二、Google 排名的四个核心信号

Google 判断"把哪个页面排第 1"主要看四件事：

```
1. 能爬到吗？  →  robots.txt + noindex + sitemap
2. 能理解吗？  →  title/description/hreflang/schema 告诉 Google 这是什么页
3. 匹配度高吗？ →  页面内容里真的有用户搜的关键词（且自然）
4. 权威吗？    →  SSL + 加载速度 + 外链 + 域名年龄
```

**GWW 当前卡在第 1 步**——Google 连看都看不到。技术优化让 1-3 步达标，权威信号靠部署和时间积累。

---

## 三、关键词矩阵（塑料原料行业）

优化的目标不是"流量"，是"**精准询盘**"。以下是需要覆盖的关键词类型：

| 类型 | 示例 | 承接页面 | 竞争程度 |
|------|------|---------|---------|
| **品牌 + 型号** | 道达尔 TPV 8400、LG化学 TPU 1285A | 产品详情页 title + description + sections | 低（品牌词自带排名） |
| **品牌 + 品类** | 道达尔 TPV 原料、中石化聚丙烯 PP、陶氏 TPO | 产品详情页 + 产品列表页筛选组合 | 中 |
| **品类关键词** | TPV 热塑性硫化橡胶、TPU 弹性体、聚丙烯 PP 原料、HDPE 聚乙烯 | 产品列表页 description + 每个产品页 | 高 |
| **应用场景** | 汽车用 TPV、建筑密封 TPU、电缆护套塑料、家电外壳 PP | 产品页 sections 里的"应用"段落 | 中 |
| **长尾 + 功能** | 耐油 TPV、低硬度 TPU、阻燃 PP、耐候 PE | 产品页 sections 里的"参数"段落 | 低（精准，转化好） |
| **公司品牌** | GWW 塑料原料、GWW 道达尔代理 | 首页 + 关于页 | 低 |

**关键词来源**：Google Keyword Planner、Ahrefs、同行站（比如做"塑料改性"的同行在页面上写的品类词）、客户实际询价的词汇。

---

## 四、三层实施路线图

### 🎯 第一层：打通收录（部署前必做）

> **目的**：让 Google 能爬、能收、能区分双语。**不做这层，后面全白搭。**
> **工作量**：1-2 天
> **预期**：部署后 1-4 周开始有收录

#### 4.1 解除全站 noindex

**改**：`website/nuxt.config.ts`

```ts
// 当前（全封锁）：
app: {
  head: {
    meta: [
      { name: "robots", content: "noindex, nofollow" },
    ],
  },
},

// 改为（prod 允许爬，dev 保持 noindex 防收录测试页）：
app: {
  head: {
    meta: [
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      // dev 环境自动加 noindex，prod 环境移除
      ...(process.env.NODE_ENV !== "production"
        ? [{ name: "robots", content: "noindex, nofollow" }]
        : []),
    ],
  },
},
```

#### 4.2 robots.txt 和 sitemap.xml（模块自动生成）

这两个**不需要手动建文件**——`@nuxtjs/robots` 和 `@nuxtjs/sitemap` 在 build/运行时自动生成：

**robots.txt 部署后实际输出**（访问 `https://yourdomain.com/robots.txt`）：
```
User-agent: *
Allow: /
Disallow: /admin
Disallow: /api/

Sitemap: https://yourdomain.com/sitemap.xml
```

**sitemap.xml 部署后实际输出**（访问 `https://yourdomain.com/sitemap.xml`）：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="http://www.w3.org/1999/xhtml">
  <!-- 首页 -->
  <url>
    <loc>https://yourdomain.com/en</loc>
    <xhtml:link rel="alternate" hreflang="zh" href="https://yourdomain.com/zh"/>
    <xhtml:link rel="alternate" hreflang="en" href="https://yourdomain.com/en"/>
    <xhtml:link rel="alternate" hreflang="x-default" href="https://yourdomain.com/en"/>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/zh</loc>
    <xhtml:link rel="alternate" hreflang="en" href="https://yourdomain.com/en"/>
  </url>
  <!-- 栏目页 -->
  <url><loc>https://yourdomain.com/zh/products</loc><xhtml:link rel="alternate" hreflang="en" href="https://yourdomain.com/en/products"/></url>
  <url><loc>https://yourdomain.com/zh/articles</loc><xhtml:link rel="alternate" hreflang="en" href="https://yourdomain.com/en/articles"/></url>
  <!-- 产品详情（来自 server/api/sitemap.ts 动态拉取后端） -->
  <url>
    <loc>https://yourdomain.com/en/products/abc-8400</loc>
    <xhtml:link rel="alternate" hreflang="zh" href="https://yourdomain.com/zh/products/abc-8400"/>
    <xhtml:link rel="alternate" hreflang="en" href="https://yourdomain.com/en/products/abc-8400"/>
    <changefreq>monthly</changefreq>
    <priority>0.7</priority>
  </url>
  <!-- 文章详情（同上） -->
  <url>
    <loc>https://yourdomain.com/en/articles/xyz-article</loc>
    <xhtml:link rel="alternate" hreflang="zh" href="https://yourdomain.com/zh/articles/xyz-article"/>
  </url>
</urlset>
```

**sitemap 动态源实现在**：[server/api/sitemap.ts](../website/server/api/sitemap.ts) — Nitro 运行时从后端 API 拉 products 和 articles，每个生成 zh/en 双语 URL。后端没启动时自动降级为静态栏目页，不中断构建。

**注意**：这两个文件是运行时生成的，不是 build 产物里的静态文件。首次部署后用浏览器直接访问 `/robots.txt` 和 `/sitemap.xml` 确认输出正确。

```powershell
cd d:\work\gww\website
npm install @nuxtjs/robots @nuxtjs/sitemap
```

在 `nuxt.config.ts` 的 `modules` 里加：

```ts
modules: ["@nuxtjs/robots", "@nuxtjs/sitemap"],

// robots.txt 配置（prod 才允许爬，dev 全站 Disallow: /）
robots: {
  Disallow: ["/admin", "/api/"],
} as any,

// sitemap 基础配置；动态产品/文章路由由 server/api/sitemap.ts 提供
site: { url: "https://yourdomain.com" },
sitemap: {
  changefreq: "weekly",
  priority: 0.8,
} as any,
```

#### 4.3 hreflang 互指（最容易漏的关键点）

多语言站点如果不做 hreflang，Google 会把 `/zh/products/8400` 和 `/en/products/8400` 当成两个**独立页面竞争排名**，互相稀释权重。

在每个页面的 `useHead` 里加：

```ts
useHead(() => ({
  // ...title/meta...
  link: [
    { rel: "canonical", href: canonicalUrl.value },
    { rel: "alternate", hreflang: "zh", href: zhUrl.value },
    { rel: "alternate", hreflang: "en", href: enUrl.value },
    { rel: "alternate", hreflang: "x-default", href: enUrl.value },
  ],
}));
```

> **经验教训**：canonical 必须用 `<link rel="canonical">`，不能写在 `<meta>` 里。全局 head 只设兜底值，页面级负责覆盖——单一来源策略避免冲突。

#### 4.4 Organization schema（全站注入）

在 `app.vue` 里加全局 JSON-LD，告诉 Google "这个站是 GWW 公司的官网"：

```vue
<script setup>
useHead({
  script: [
    {
      type: "application/ld+json",
      children: JSON.stringify({
        "@context": "https://schema.org",
        "@type": "Organization",
        name: "GWW",
        url: "https://yourdomain.com",
        logo: "https://yourdomain.com/images/logo.png",
        contactPoint: {
          "@type": "ContactPoint",
          email: "info@gww.com",
          contactType: "sales",
        },
      }),
    },
  ],
});
</script>
```

#### 4.5 部署后操作

| 步骤 | 说明 |
|------|------|
| **Search Console 域名验证** | 用 Google 账号登录 [search.google.com/search-console](https://search.google.com/search-console)，DNS 验证最快 |
| **提交 sitemap.xml** | Search Console → Sitemaps → 填 `https://yourdomain.com/sitemap.xml` |
| **URL Inspector 测试** | 拿一个产品页 URL（如 `/zh/products/{id}`），点"测试实时索引"——看 Google 能不能渲染出完整 HTML（不是空壳） |
| **提交所有产品 URL** | 初次提交后，Google 可能只爬首页和列表页。用 URL Inspector 批量提交产品详情页 |

---

### 🎯 第二层：页面独特身份（让 Google 懂这是什么页）

> **目的**：每个页面告诉 Google "我是谁"。现在全站 title/description 高度同质化——Google 看到几十个页面写着一样的描述，不知道该把哪个排前面。
> **工作量**：2-3 天
> **前提**：第一层做完

#### 4.6 产品详情页 SEO 改造（最核心）

**文件**：`website/app/pages/[locale]/[[...slug]].vue`（`view.type === 'product'` 分支）

**title 模板**：
```ts
// 当前：`${category} ${model}` — 太干
// 改为：品牌 + 型号 + 类别 + 公司名
const title = computed(() => {
  if (view.value?.type === "product") {
    const p = product.value;
    if (!p) return "产品详情";
    const brand = p.brand ? `${p.brand} ` : "";
    const category = p.category ? ` ${p.category}` : "";
    return locale.value === "en"
      ? `${brand}${p.model}${category} | GWW Specialty Materials`
      : `${brand}${p.model}${category} | GWW 塑料原料供应商`;
  }
  // ...其他类型的 title
});
```

**description 模板**：
```ts
// 当前：全写死 t.value.intro — 所有页面一模一样
// 改为：优先用产品自身的 description，后台没填时自动拼接
const pageDescription = computed(() => {
  if (view.value?.type === "product") {
    const p = product.value;
    if (!p) return "";
    const userDesc = locale.value === "en" ? p.descriptionEn : p.description;
    if (userDesc && userDesc.trim()) {
      // 截断到 150-160 字符（Google 展示框的理想长度）
      return userDesc.length > 160 ? userDesc.slice(0, 157) + "..." : userDesc;
    }
    // 自动兜底：品牌 + 型号 + 品类 + GWW
    const brand = p.brand || "";
    const model = p.model || "";
    const category = p.category || "";
    return locale.value === "en"
      ? `GWW supplies ${brand} ${model} ${category}. Contact us for pricing and availability.`
      : `GWW 代理${brand} ${model} ${category}塑料原料，欢迎咨询报价和现货情况。`;
  }
  // 列表页/关于页/联系页的独立描述...
  return defaultDescription;
});
```

**完整 useHead**：
```ts
useHead(() => {
  const zhPath = route.path.replace(`/${locale.value}/`, "/zh/");
  const enPath = route.path.replace(`/${locale.value}/`, "/en/");
  return {
    title: title.value,
    meta: [
      { name: "description", content: pageDescription.value },
      { name: "keywords", content: pageKeywords.value }, // 可选，Google 基本忽略但无害
      // Open Graph（社交分享预览）
      { property: "og:title", content: title.value },
      { property: "og:description", content: pageDescription.value },
      { property: "og:type", content: view.value?.type === "product" ? "product" : "website" },
      { property: "og:locale", content: locale.value === "zh" ? "zh_CN" : "en_US" },
      // Twitter Card
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: title.value },
      { name: "twitter:description", content: pageDescription.value },
    ],
    link: [
      { rel: "canonical", href: `https://yourdomain.com${route.path}` },
      { rel: "alternate", hreflang: "zh", href: `https://yourdomain.com${zhPath}` },
      { rel: "alternate", hreflang: "en", href: `https://yourdomain.com${enPath}` },
      { rel: "alternate", hreflang: "x-default", href: `https://yourdomain.com${enPath}` },
    ],
  };
});
```

#### 4.7 产品列表页 / 关于页 / 联系页 meta

| 页面 | title 模板 | description 模板 |
|------|-----------|-----------------|
| 产品中心 | `塑料原料产品中心 | GWW 代理品牌塑料原料` | `"GWW 专业代理道达尔、中石化、LG等品牌的 TPV、TPU、PP、PE 等塑料原料..."` |
| 关于我们 | `关于 GWW | 塑料原料进口供应商` | `"GWW 是一家专注于 specialty plastics 进口分销的公司..."` |
| 品牌页 | `代理品牌 | GWW 塑料原料品牌合作伙伴` | `"GWW 代理道达尔、中石化、LG化学、陶氏等国际品牌塑料原料..."` |
| 联系我们 | `联系 GWW | 获取塑料原料报价` | `"获取 GWW 塑料原料报价、产品咨询和样品申请..."` |

#### 4.8 Product schema（每个产品详情页）

```ts
// 产品详情页的 JSON-LD
useHead(() => ({
  script: [{
    type: "application/ld+json",
    children: JSON.stringify({
      "@context": "https://schema.org",
      "@type": "Product",
      name: product.value?.model,
      brand: { "@type": "Brand", name: product.value?.brand },
      category: product.value?.category,
      image: product.value?.image ? [`https://yourdomain.com${product.value.image}`] : [],
      description: pageDescription.value,
      offers: {
        "@type": "Offer",
        availability: "https://schema.org/InStock",
        url: `https://yourdomain.com${route.path}`,
      },
    }),
  }],
}));
```

#### 4.9 BreadcrumbList schema（面包屑）

```ts
// 每个内页注入面包屑，Google 搜索结果会显示面包屑路径
useHead(() => ({
  script: [{
    type: "application/ld+json",
    children: JSON.stringify({
      "@context": "https://schema.org",
      "@type": "BreadcrumbList",
      itemListElement: [
        { "@type": "ListItem", "position": 1, name: "首页", item: `https://yourdomain.com/${locale.value}` },
        ...(view.value?.type === "product" ? [
          { "@type": "ListItem", "position": 2, name: "产品中心", item: `https://yourdomain.com/${locale.value}/products` },
          { "@type": "ListItem", "position": 3, name: product.value?.model },
        ] : []),
      ],
    }),
  }],
}));
```

---

### 🎯 第三层：部署 & 权威信号优化

> **目的**：SSL + 加载速度 + 外链。这些不直接决定收录，但影响排名上浮速度。
> **工作量**：部署当天搞定 SSL + Nginx 优化
> **外链**：长期，主动出击

#### 4.10 SSL 部署（必做）

Google 2017 年起将 HTTPS 作为排名因子。部署时 Nginx 加：

```nginx
# HTTP → HTTPS 强制跳转
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://www.yourdomain.com$request_uri;
}

server {
    listen 443 ssl;
    server_name www.yourdomain.com;

    # Let's Encrypt 自动续期
    ssl_certificate     /etc/letsencrypt/live/www.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.yourdomain.com/privkey.pem;

    # HSTS：告诉浏览器以后只走 HTTPS（一年）
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options nosniff always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;

    # ... 其他 proxy_pass 配置保持不变 ...
}
```

> 经验：不要在 robots.txt 里禁止 CSS/JS/图片——现代搜索引擎需要这些资源来渲染和理解页面。

#### 4.11 Nginx 静态资源缓存（加载速度）

```nginx
server {
    # ... SSL 配置 ...

    # Nuxt SSR 产物的静态资源（_nuxt/ 下全是带 hash 的文件名，可以永久缓存）
    location /_nuxt/ {
        alias /var/www/gww/.output/public/_nuxt/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 通用静态资源缓存 1 周
    location ~* \.(jpg|jpeg|png|webp|gif|svg|css|js|woff2?)$ {
        alias /var/www/gww/.output/public/;
        expires 7d;
        add_header Cache-Control "public";
    }

    # SSR 页面走 proxy_pass（不缓存或短缓存）
    location / {
        proxy_pass http://127.0.0.1:3100;
        proxy_set_header Host $host;
        # ... 其他 proxy 头 ...
    }
}
```

#### 4.12 admin CSS 瘦身（可选优化）

当前 build 产物中 `admin-styles-*.css` 有 354KB。Element Plus 默认全量引入，改成按需引入可减到 ~50KB：

```ts
// nuxt.config.ts
modules: [
  ...
  "@element-plus/nuxt",
],
elementPlus: {
  // 按需引入，减少打包体积
  importStyle: "scss",
},
```

#### 4.13 外链建设（长期）

| 方法 | 难度 | 速度 | 备注 |
|------|------|------|------|
| Google Business Profile | 1/5 | 1 天 | 注册为"塑料原料供应商"，地图+本地搜索展示 + 官方外链 |
| B2B 平台注册 | 2/5 | 1 周 | Made-in-China、Alibaba、环球资源——每注册一个拿一个外链 |
| 公司主页同步 | 1/5 | 1 天 | LinkedIn、Facebook、Twitter/X 公司页 + 官网链接 |
| 行业展会/协会 | 3/5 | 1-3 月 | 橡塑展、行业协会会员 → 让他们加"会员单位"链接 |
| 友链交换 | 4/5 | 慢 | 同行（非直接竞品）或上下游供应商互相链接 |
| 旧域名 301 | — | 立即 | 公司有旧官网的话，301 跳转传递 80-90% 旧权重 |

> ⚠️ 绝对不要买垃圾外链 / PBN（私人博客网络）。Google 2024 年对垃圾外链打击极严，会触发手动处罚。

---

## 五、关键词结构模板（后台可直接复制粘贴）

> 按品类给具体的 description 和 sections 写法。每个产品填完后，Google 能自然命中品牌词、型号词、品类词、应用词、长尾词五类搜索。

### 5.1 模板一：TPV 热塑性硫化橡胶（热塑性弹性体的核心品类）

**后台产品字段填写：**

| 字段 | 中文 | 英文 |
|------|------|------|
| **name / nameEn** | 道达尔 TPV 8400 | TotalEnergies TPV 8400 |
| **brand** | 道达尔 | TotalEnergies |
| **category** | TPV | TPV |
| **description** | 道达尔 TPV 8400 是一款高弹性热塑性硫化橡胶（TPV），具有优异的耐候性、耐油性和抗紫外线性能，广泛应用于汽车密封条、建筑密封件、电线电缆护套、工业软管等领域。GWW 提供现货供应和技术咨询。 | TotalEnergies TPV 8400 is a high-performance thermoplastic vulcanizate (TPV) with excellent weather resistance, oil resistance and UV stability, widely used in automotive weatherstripping, building seals, wire and cable sheathing, industrial hoses and other applications. GWW offers stock supply and technical consultation. |

**sections 段落（每段 = 标题 + 正文，会渲染成独立 `<h2>` + `<p>` 块）：**

| 段序 | 中文标题 | 中文正文 | 英文标题 | 英文正文 |
|------|---------|---------|---------|---------|
| 1 | 物性参数 Properties | 道达尔 TPV 8400 硬度 40A，密度 0.96 g/cm³，拉伸强度 8 MPa，断裂伸长率 450%，耐温范围 -40°C ~ 125°C，符合 ASTM D2240 测试标准。 | Properties | TotalEnergies TPV 8400 has a hardness of 40A, density 0.96 g/cm³, tensile strength 8 MPa, elongation at break 450%, temperature range -40°C to 125°C. Tested per ASTM D2240. |
| 2 | 应用领域 Applications | TPV 8400 主要应用于：汽车行业（车门密封条、防尘罩、天窗密封）；建筑行业（门窗密封件、幕墙防水条）；工业领域（电线电缆护套、液压软管、垫片密封）。 | Applications | TPV 8400 is primarily used in: Automotive (door weatherstrips, dust boots, sunroof seals); Building & Construction (window/door seals, curtain wall gaskets); Industrial (wire & cable sheathing, hydraulic hoses, gasket seals). |
| 3 | 材料特性 Features | 作为一种无需硫化的热塑性弹性体，TPV 8400 可回收再利用，具有低温柔性好、耐臭氧、耐溶剂、加工效率高的特点，适合注塑和挤出成型工艺。 | Features | As a non-vulcanized thermoplastic elastomer, TPV 8400 is fully recyclable, with excellent low-temperature flexibility, ozone resistance, solvent resistance and high processability. Suitable for injection molding and extrusion. |

### 5.2 模板二：TPU 热塑性聚氨酯弹性体

| 字段 | 中文 | 英文 |
|------|------|------|
| **description** | LG化学 TPU 1285A 是一款脂肪族热塑性聚氨酯（TPU），具有出色的耐磨性、抗冲击性和耐水解性，硬度范围 85A-95A 可调，广泛用于工业滚轮、密封件、软管护套、鞋材、电缆护套等领域。 | LG Chem TPU 1285A is an aliphatic thermoplastic polyurethane (TPU) with excellent abrasion resistance, impact strength and hydrolysis resistance. Available in hardness range 85A-95A, widely used in industrial rollers, seals, hose sheaths, footwear and cable sheathing. |

**sections 模板：**

| 段序 | 中文标题 | 中文正文 |
|------|---------|---------|
| 1 | 性能参数 Specifications | TPU 1285A 密度 1.21 g/cm³，邵氏硬度 85A，拉伸强度 45 MPa，断裂伸长率 550%，磨耗值 35 mm³（DIN 53516），使用温度范围 -40°C ~ 120°C。 |
| 2 | 应用场景 Applications | 工业领域（输送机滚轮、液压密封件、气动软管）；建筑领域（门窗密封条、幕墙防水）；消费品（运动鞋中底、手机壳）；电气领域（电线电缆护套、插头外壳）。 |
| 3 | 加工建议 Processing | TPU 1285A 推荐注塑温度 180-210°C，模具温度 30-60°C；挤出温度 170-200°C。加工前需干燥至水分含量低于 0.05%。 |

### 5.3 模板三：聚丙烯 PP（通用塑料最大品类）

| 字段 | 中文 | 英文 |
|------|------|------|
| **description** | 中石化 T30S 是一款均聚聚丙烯（PP Homopolymer），具有良好的刚性、耐热性和耐化学腐蚀性，熔融指数 3.0 g/10min，广泛应用于食品包装、家电外壳、汽车内饰、工业容器等领域。 | Sinopec T30S is a homopolymer polypropylene (PP) with good rigidity, heat resistance and chemical resistance. Melt flow index 3.0 g/10min, widely used in food packaging, home appliance enclosures, automotive interiors and industrial containers. |

**sections 模板：**

| 段序 | 中文标题 | 中文正文 |
|------|---------|---------|
| 1 | 物性指标 Properties | 聚丙烯 T30S 密度 0.91 g/cm³，熔融指数 3.0 g/10min（230°C/2.16kg），拉伸屈服强度 36 MPa，弯曲模量 1500 MPa，热变形温度 105°C。 |
| 2 | 典型应用 Applications | 食品包装（餐盒、薄膜、瓶盖）；家用电器（洗衣机面板、空调外壳、微波炉部件）；汽车行业（仪表盘饰板、门板、空调出风口）；工业物流（周转箱、塑料托盘）。 |
| 3 | 材料特性 Features | 均聚聚丙烯相比共聚 PP 具有更高的刚性和耐热性，但抗冲击性能稍差。T30S 符合 GB/T 12670 国家标准，食品接触合规（GB 4806.6）。 |

### 5.4 模板四：聚烯烃弹性体 POE / TPO

| 字段 | 中文 | 英文 |
|------|------|------|
| **description** | 陶氏 Engage POE 8150 是一款茂金属催化的聚烯烃弹性体（POE），具有优异的耐低温冲击性能和良好的橡胶弹性，常作为聚丙烯 PP 的增韧改性剂，广泛用于汽车保险杠、仪表板、家电外壳等抗冲击要求高的领域。 | Dow Engage POE 8150 is a metallocene-catalyzed polyolefin elastomer (POE) with excellent low-temperature impact resistance and rubber-like elasticity. Commonly used as an impact modifier for polypropylene PP, widely used in automotive bumpers, instrument panels and home appliance enclosures. |

---

### 5.5 关键词覆盖率对照表

填完以上内容后，每个产品页自然覆盖的关键词：

| 关键词类型 | 从哪里来 | 例子（道达尔 TPV 8400） |
|-----------|---------|------------------------|
| **品牌 + 型号** | title + description 第一句 | "道达尔 TPV 8400"、"道达尔 TPV 8400 塑料原料" |
| **品类词** | description + sections | "热塑性硫化橡胶"、"TPV 弹性体"、"TPV 原料" |
| **品牌 + 品类** | description | "道达尔 TPV"、"TotalEnergies TPV" |
| **应用场景** | sections 第 2 段 | "汽车密封条塑料"、"建筑密封 TPV"、"电缆护套材料" |
| **功能/长尾** | sections 第 1、3 段 | "耐油 TPV"、"低温柔性 TPV"、"可回收热塑性弹性体" |
| **公司品牌** | 全站 title 后缀 + description 末尾 | "GWW 道达尔代理"、"GWW 塑料原料供应商" |

### 5.6 文章（资讯）关键词结构

文章覆盖**品类级大词**，和产品页的**精准长尾词**互补：

| 文章标题示例 | 覆盖关键词 | 推荐分类 |
|-------------|-----------|---------|
| 《TPV 热塑性硫化橡胶详解：材料特性、应用领域与选型指南》 | TPV 是什么、热塑性硫化橡胶、TPV 应用、TPV 选型 | 材料百科 |
| 《汽车行业用 TPV 密封材料——为什么越来越多车企选择 TPV 替代传统橡胶？》 | 汽车 TPV、汽车密封条材料、TPV vs 橡胶 | 行业应用 |
| 《TPU 弹性体 vs TPV：两种热塑性弹性体的区别与选型建议》 | TPU vs TPV、热塑性弹性体对比、TPU 选型 | 技术对比 |
| 《聚丙烯 PP 的改性方法汇总——增韧、增强、阻燃、抗老化》 | 聚丙烯改性、PP 增韧、PP 阻燃、PP 增强 | 材料加工 |
| 《2025 年全球热塑性弹性体（TPE/TPV/TPU）市场趋势与供应展望》 | TPE 市场、TPV 趋势、热塑性弹性体供应 | 行业资讯 |

每篇文章结构建议（后台 sections）：
- **Section 1 概述**：定义 + 核心特性 + 行业地位（≈200 字）
- **Section 2 详细内容**：分点展开，嵌入关键词（≈500-800 字）
- **Section 3 应用/选型建议**：结合 GWW 代理的品牌/型号（自然带出产品链接）
- **Section 4 总结**：3-5 点提炼

---

## 六、后台产品内容填充指南

> **这是整个 SEO 最关键的部分。** 技术是基础，但页面内容里真的有用户搜的关键词，才是排名的核心。
> 后台产品编辑页面已有 `description` 和 `sections`（JSON 结构化段落）字段，直接用。

### 5.1 description 字段写法

| ❌ 不要这样 | ✅ 要这样 |
|-------------|----------|
| `TPV 8400` | `道达尔 TPV 8400 是一款高弹性热塑性硫化橡胶（TPV），具有优异的耐候性和耐油性，广泛应用于汽车密封条、建筑密封件、电线电缆护套等领域。GWW 提供现货供应和技术咨询。` |
| `规格见参数表` | 同上——自然嵌入品牌、型号、品类、应用关键词 |

**写法要点**：
- 第一句必须出现 `品牌 + 型号 + 品类`
- 第二句加 `核心性能 / 特点`
- 第三句加 `典型应用场景`
- 最后可以加 `GWW 提供...` 自然带出品牌

### 5.2 sections 段落填充

sections 是独立的 `<h2>` + `<p>` 块，Google 会识别为独立内容块。**不要留空**：

推荐每个产品至少 2-3 个 section：

| Section 标题（中/英） | 内容 |
|----------------------|------|
| **参数 / Properties** | 密度、硬度、拉伸强度、耐温范围、物性表摘要 |
| **应用 / Applications** | 汽车行业（密封条、防尘罩）、建筑行业（密封件、门窗）、工业领域（电缆护套、软管） |
| **特性 / Features** | 耐候性、耐油性、低温柔性、可回收、加工方式 |
| **加工 / Processing** | 注塑、挤出、吹塑——加工温度、模具温度建议 |

### 5.3 列表页 description

产品列表页（`/zh/products`）的 SEO 描述应该总结整个品类，不是单条产品：

> `GWW 专业代理道达尔、中石化、LG化学、陶氏等国际品牌的塑料原料，覆盖 TPV 热塑性硫化橡胶、TPU 弹性体、聚丙烯 PP、聚乙烯 PE、TPO 等品类，广泛应用于汽车、建筑、工业、家电等领域。`

---

## 六、时间线与预期

| 阶段 | 时间 | 预期效果 | 依赖 |
|------|------|---------|------|
| 技术基建 | 部署前 | robots + sitemap + hreflang + schema 全部就绪 | 无 |
| 收录 | 部署后 1-4 周 | Search Console 显示"已编入索引" | Search Console 提交 |
| 品牌词排名 | 1-3 个月 | "GWW 塑料原料"、"道达尔 TPV 8400" 排前 10 | 产品页 description 已填充 |
| 品类词排名 | 3-6 个月 | "TPV 原料供应商"、"汽车用 TPV" 有排名 | sections 已填充 + 少量外链 |
| 稳定排名 | 6-12 个月 | 竞争大的词逐步上来，配合外链稳步提升 | 外链积累 + 域名年龄 |

### 新域名的"软沙盒期"

Google 对新站前 3-6 个月有观察期——做了 SEO 但排名不动是正常的，不是没做对。**优势**在于：如果你的产品页真的比同行站内容丰富（每个产品都有参数、应用、品牌、型号、描述），Google 反而愿意给新站排名来"测试"。

---

## 七、验收清单

部署后检查这些：

- [ ] `https://yourdomain.com/robots.txt` 返回 200，内容正确
- [ ] `https://yourdomain.com/sitemap.xml` 返回 200，包含所有产品 URL
- [ ] 产品页 HTML 源代码里有正确的 `<title>` 和 `<meta name="description">`
- [ ] 产品页 HTML 源代码里有 `<link rel="canonical">` 和 hreflang 互指
- [ ] 产品页 HTML 源代码里有 `application/ld+json` 的 Product schema
- [ ] Search Console 里 "URL Inspector" 测试产品页能看到完整 HTML（不是空壳）
- [ ] Search Console 里 "页面索引" 报告显示产品页已编入索引
- [ ] `https://yourdomain.com` 自动跳转到 HTTPS

---

## 八、已完成的代码改动清单

> 以下文件在本次会话中已全部修改完成，`nuxi typecheck` + `build` 全绿 ✅

### 前端（Website）

| 文件 | 改动内容 | 状态 |
|------|---------|------|
| `website/nuxt.config.ts` | 加 `@nuxtjs/robots` + `@nuxtjs/sitemap` 模块；dev 保持 noindex、prod 允许爬；runtimeConfig 加 `siteUrl` | ✅ |
| `website/server/api/sitemap.ts` | **新建** sitemap 动态数据源——运行时从后端拉 products + articles，每条生成 `/zh/xxx` + `/en/xxx` + hreflang | ✅ |
| `website/app/pages/[locale]/[[...slug]].vue` | **重写** SEO 全套：动态 title / description / keywords / canonical / hreflang 三向互指 / OG / Twitter Card / BreadcrumbList + Product + Article JSON-LD；同时加 articles 列表页 + 详情页渲染 | ✅ |
| `website/app/app.vue` | 导航 links 数组加 `"articles"` | ✅ |
| `website/app/data/catalog.ts` | sections 加 `"articles"`；nav 加"行业资讯/News"；resolvePath 支持 `/articles/{id}` | ✅ |
| `website/app/components/AdminDashboard.vue` | 后台加"资讯管理"菜单；loadRecords 同时加载 products + articles；save() sections articles 也传；列表/编辑对话框 articles 共用 | ✅ |

### 后端（无需改动）

| 文件 | 说明 |
|------|------|
| `backend/server/src/main/resources/db/migration/V1__initial.sql` | `content_record` 通用表，`kind` 字段区分 `products` / `articles`，已支持 |
| `backend/server/src/main/resources/db/migration/V3__content_sections.sql` | `sections` JSON 列，产品和文章共用，已存在 |
| `backend/.../ContentController.java` | `/api/admin/content/{kind}` 和 `/api/public/content/{kind}` 通用 CRUD，products + articles 都能走，无需改 |

### 部署后必做（运营）

| 步骤 | 说明 | 谁做 |
|------|------|------|
| **改域名占位** | `nuxt.config.ts` 里 `siteUrl`、`server/api/sitemap.ts` 里 `base` → 替换为真实域名 | 部署时 |
| **SSL 证书** | Let's Encrypt + Nginx 强制 HTTPS 跳转 | 服务器运维 |
| **robots.txt 验证** | 浏览器访问 `https://yourdomain.com/robots.txt` 确认输出正确 | 部署后 |
| **sitemap.xml 验证** | 浏览器访问 `https://yourdomain.com/sitemap.xml` 确认包含产品+文章 URL | 部署后 |
| **Google Search Console** | 域名验证 → 提交 sitemap.xml → URL Inspector 测试产品页渲染 | 运营 |
| **Bing Webmaster Tools** | 域名验证 → 提交 sitemap.xml（DuckDuckGo 自动继承） | 运营 |
| **百度资源平台** | 域名验证 → 提交 sitemap.xml + 配置主动推送 token（见另一个方案文档） | 运营 |
| **后台内容填充** | 按本方案第五章的模板，逐条产品填 description + sections；每周 1-2 篇资讯文章 | 运营（长期） |
| **外链建设** | Google Business Profile + B2B 平台注册 + 行业链接 | 运营（长期） |

export type Locale = "zh" | "en";
export const locales: Locale[] = ["zh", "en"];
export const sections = [
  "about",
  "products",
  "brands",
  "news",
  "contact",
] as const;

export const text = {
  zh: {
    name: "材料展示",
    tagline: "SPECIALTY MATERIALS",
    nav: [
      "首页",
      "关于我们",
      "产品中心",
      "品牌展示",
      "新闻资讯",
      "联系我们",
    ] as const,
    eyebrow: "面向应用 · 连接材料",
    hero: "探索材料的更多可能",
    intro: "从材料型号到应用需求，让每一次选材都有清晰的起点。",
    browse: "探索产品",
    contact: "联系我们",
    featured: "精选材料",
    featuredSub: "由你提供的产品图片构建的展示目录，技术规格将在确认后补充。",
    all: "全部",
    detail: "查看详情",
    brand: "品牌",
    category: "材料类别",
    model: "产品型号",
    search: "搜索型号、品牌或关键词",
    empty: "没有匹配的产品，请调整筛选条件。",
    reset: "清除筛选",
    companyTitle: "以材料为起点，以应用为方向",
    companyText:
      "这里将展示企业介绍、材料供应能力与服务理念。当前为一期展示网站，企业名称、联系方式和技术资料待确认，不构成供货或代理承诺。",
    advantages: ["清晰的产品目录", "面向应用的沟通", "多语言内容展示"],
    advantageText: [
      "按型号快速找到材料信息。",
      "从工艺与使用场景开始交流。",
      "为跨地区业务提供一致的浏览体验。",
    ] as const,
    brandsTitle: "材料品牌展示",
    brandsNote:
      "品牌图片由用户提供，仅用于页面展示，不代表本企业拥有代理、合作或认证授权。",
    note: "一期展示版 · 企业信息与技术数据待确认",
    sample: "展示样例",
    read: "阅读文章",
    news: "行业与企业资讯",
    knowledge: "从认识材料开始",
    honors: "荣誉与资质",
    pending: "资料待补充",
    honorsText: "真实资质审核确认后将在此展示，不使用示例证书或虚构授权。",
    contactTitle: "让材料沟通更简单",
    contactText:
      "请先配置企业联系方式或人工客服渠道。当前不提供留言提交，不会保存个人信息。",
    phone: "联系电话",
    email: "电子邮箱",
    support: "在线客服",
    unconfigured: "暂未配置",
    home: "返回首页",
    parameters: "技术参数",
    parametersText:
      "密度、熔体指数、测试标准等参数尚未提供。请以经企业确认的技术资料为准。",
    application: "应用说明",
    applicationText:
      "本页用于展示产品详情结构。应用领域及适用性需结合真实技术资料和实际工况确认。",
    previous: "上一页",
    next: "下一页",
    results: "个结果",
    notFound: "页面不存在",
    notFoundText: "链接可能已变更，请返回首页继续浏览。",
    pause: "暂停轮播",
    play: "播放轮播",
    slide: "轮播图",
    menu: "切换导航",
    top: "返回顶部",
  },
  en: {
    name: "Materials Showcase",
    tagline: "SPECIALTY MATERIALS",
    nav: [
      "Home",
      "About",
      "Products",
      "Brands",
      "News",
      "Contact",
    ],
    eyebrow: "APPLICATIONS MEET MATERIALS",
    hero: "Explore what materials can make possible",
    intro:
      "A clear starting point for exploring material grades and application needs.",
    browse: "Explore products",
    contact: "Contact us",
    featured: "Featured materials",
    featuredSub:
      "A showcase using your supplied product images. Verified specifications will be added later.",
    all: "All",
    detail: "View details",
    brand: "Brand",
    category: "Material type",
    model: "Product grade",
    search: "Search grades, brands or keywords",
    empty: "No matching products. Please adjust your filters.",
    reset: "Clear filters",
    companyTitle: "Materials first. Applications in focus.",
    companyText:
      "This section will introduce the company, its supply capabilities and service approach. This is a demonstration website; company details and technical information are pending verification, not a supply or agency commitment.",
    advantages: [
      "A clear product catalog",
      "Application-led conversations",
      "Multilingual content display",
    ],
    advantageText: [
      "Find material information by grade.",
      "Start with processing and application needs.",
      "A consistent experience across markets.",
    ],
    brandsTitle: "Material brand showcase",
    brandsNote:
      "The supplied brand image is for display only. It does not imply agency, partnership or certification.",
    note: "Demonstration · Company details and technical data pending",
    sample: "Demo content",
    read: "Read article",
    news: "Industry and company updates",
    knowledge: "Start with material insights",
    honors: "Credentials",
    pending: "Information pending",
    honorsText:
      "Verified credentials will be shown here. No sample certificates or invented authorizations are used.",
    contactTitle: "Start a conversation about materials",
    contactText:
      "Company contact details or a human support channel must be configured first. This version has no inquiry submission and stores no personal information.",
    phone: "Phone",
    email: "Email",
    support: "Contact support",
    unconfigured: "Not configured",
    home: "Back to home",
    parameters: "Technical specifications",
    parametersText:
      "Density, melt flow index and testing standards have not been supplied. Refer to verified technical documentation.",
    application: "Applications",
    applicationText:
      "This page demonstrates the product detail layout. Applications and suitability must be verified against real technical documentation and operating conditions.",
    previous: "Previous",
    next: "Next",
    results: "results",
    notFound: "Page not found",
    notFoundText:
      "The link may have changed. Return to the home page to continue.",
    pause: "Pause carousel",
    play: "Play carousel",
    slide: "Slide",
    menu: "Toggle navigation",
    top: "Back to top",
  },
};

export const products = [
  {
    id: "8400",
    model: "8400",
    category: "POE",
    brand: "DOW",
    image: "8400.jpg",
  },
  {
    id: "8440",
    model: "8440",
    category: "POE",
    brand: "DOW",
    image: "8440.jpg",
  },
  {
    id: "8450",
    model: "8450",
    category: "POE",
    brand: "DOW",
    image: "8450jpg.jpg",
  },
  {
    id: "8480",
    model: "8480",
    category: "POE",
    brand: "DOW",
    image: "8480.jpg",
  },
  {
    id: "8540",
    model: "8540",
    category: "POE",
    brand: "DOW",
    image: "8540.jpg",
  },
  {
    id: "8842",
    model: "8842",
    category: "POE",
    brand: "DOW",
    image: "8842.jpg",
  },
];

export const articles = [
  {
    id: "choosing-materials",
    kind: "knowledge",
    title: {
      zh: "材料选型前，需要准备哪些信息？",
      en: "What information helps start material selection?",
    },
    summary: {
      zh: "从应用、工艺与测试条件出发，建立清晰的沟通基础。",
      en: "Start with applications, processing and test conditions.",
    },
    paragraphs: {
      zh: [
        "材料选型需要先明确应用环境、加工工艺以及性能目标。单一型号或典型性能数值，通常不足以判断最终适用性。",
        "沟通时可准备现用材料、工艺条件、测试标准和目标性能。技术资料、样品测试和供应方确认应共同构成决策依据。",
      ],
      en: [
        "Material selection starts with the operating environment, processing method and performance targets. A grade name or a typical value alone is not enough to confirm suitability.",
        "Prepare the current material, processing conditions, test standards and target performance. Verified documentation, sample testing and supplier confirmation support the decision.",
      ],
    },
  },
  {
    id: "technical-documents",
    kind: "knowledge",
    title: {
      zh: "如何阅读材料技术资料？",
      en: "How to read material technical documentation",
    },
    summary: {
      zh: "关注单位、测试方法、条件和资料版本，而不只是数值。",
      en: "Look at units, methods, conditions and document versions.",
    },
    paragraphs: {
      zh: [
        "对比材料参数时，应同时检查测试方法、单位和测试条件。不同条件下的数值不宜直接比较。",
        "典型值不等于保证规格。涉及认证、合规和具体应用的判断，应核实最新文件，并由相关专业人员确认。",
      ],
      en: [
        "When comparing properties, check the methods, units and test conditions together. Values measured under different conditions may not be directly comparable.",
        "Typical values are not guaranteed specifications. Verify current documents for certifications, compliance and specific applications.",
      ],
    },
  },
  {
    id: "website-preview",
    kind: "news",
    title: {
      zh: "材料展示网站：一期页面预览",
      en: "Materials showcase: first-stage preview",
    },
    summary: {
      zh: "产品、品牌与中英文页面现已组成可浏览的展示目录。",
      en: "Browse product images, brands and bilingual demonstration pages.",
    },
    paragraphs: {
      zh: [
        "本网站当前为一期A步展示版本，页面使用本地演示数据。产品图片与品牌图片由用户提供。",
        "本次预览以页面、材料图片和中英文浏览体验为主，正式企业信息与技术资料将在审核确认后完善。",
      ],
      en: [
        "This is the stage A demonstration, using local content. Product and brand images were supplied by the user.",
        "Administration, verified inquiries and OSS uploads are planned for later stages. They are not available in this version.",
      ],
    },
  },
];

/**
 * 查询本地演示产品，B步可替换为公共API实现。
 * query匹配型号/类别/品牌，忽略大小写及首尾空格；
 * category、brand为空表示不限制，各条件同时满足才返回产品。
 */
export function filterProducts(query = "", category = "", brand = "") {
  const term = query.trim().toLowerCase();
  return products.filter(
    (product) =>
      (!category || product.category === category) &&
      (!brand || product.brand === brand) &&
      `${product.model} ${product.category} ${product.brand}`
        .toLowerCase()
        .includes(term),
  );
}

/**
 * 将语言之后的路径数组解析为页面类型和详情ID。
 * 空数组代表首页；仅接受已定义栏目及存在的详情，不匹配则返回null供页面显示404。
 */
export function resolvePath(parts: string[]) {
  if (parts.length === 0) return { type: "home", id: "" };
  if (
    parts.length === 1 &&
    sections.includes(parts[0] as (typeof sections)[number])
  )
    return { type: parts[0]!, id: "" };
  if (
    parts.length === 2 &&
    parts[0] === "products" &&
    products.some((p) => p.id === parts[1])
  )
    return { type: "product", id: parts[1]! };
  if (
    parts.length === 2 &&
    parts[0] === "news" &&
    articles.some((a) => a.id === parts[1])
  )
    return { type: "article", id: parts[1]! };
  return null;
}

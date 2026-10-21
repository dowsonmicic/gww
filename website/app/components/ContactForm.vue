<script setup lang="ts">
import type { Locale } from "~/data/catalog";
const props = defineProps<{
  locale: Locale;
}>();
const api = useJavaApi();
const busy = ref(false);
const feedback = ref("");
const success = ref(false);
const form = reactive({
  name: "",
  email: "",
  phone: "",
  country: "",
  industry: "",
  company: "",
  message: "",
});
const labels = computed(() =>
  props.locale === "zh"
    ? {
      title: "联系我们",
      intro: "从材料选型到应用需求，欢迎与我们交流。",
      guidance: "告诉我们您的应用场景、材料需求或采购计划，我们会根据留言与您联系。",
      formTitle: "发送咨询",
      requiredNote: "带 * 的项目为必填项",
      // stepsTitle: "让我们更了解您的需求",
      // steps: ["介绍您的应用场景", "说明材料或采购需求", "留下方便联系的方式"],
      messageHint: "请描述应用用途、所需材料、预计用量或您想了解的问题。",
      name: "姓名",
      email: "邮箱",
      phone: "电话",
      country: "选择国家/地区",
      industry: "选择行业",
      company: "公司名称",
      message: "留言内容",
      submit: "发送留言",
      sending: "正在提交",
      success: "提交成功，我们会尽快联系您。",
      failure: "提交失败，请稍后重试。",
      privacy: "提交即表示同意我们使用所填信息回复本次咨询。",
      other: "其他",
      countries: ["越南", "中国", "泰国", "马来西亚", "新加坡"],
      industries: ["塑料加工", "汽车", "包装", "电子电器", "建筑"],
    }
    : {
      title: "Contact Us",
      intro: "Let's talk about materials and your next application.",
      guidance: "Share your application, material requirements or purchasing plans, and we will get back to you.",
      formTitle: "Send an Inquiry",
      requiredNote: "Fields marked * are required",
      // stepsTitle: "Help us understand your needs",
      // steps: ["Tell us about your application", "Describe your material requirements", "Leave your preferred contact details"],
      messageHint: "Tell us about your application, materials, estimated quantity or questions.",
      name: "Name",
      email: "Email",
      phone: "Phone No.",
      country: "Select Country / Region",
      industry: "Select Industry",
      company: "Company Name",
      message: "Message",
      submit: "Send Now",
      sending: "Sending",
      success: "Thank you. We will contact you soon.",
      failure: "Unable to submit. Please try again later.",
      privacy:
        "By submitting, you agree that we may use these details to reply to your inquiry.",
      other: "Other",
      countries: ["Vietnam", "China", "Thailand", "Malaysia", "Singapore"],
      industries: [
        "Plastics processing",
        "Automotive",
        "Packaging",
        "Electronics",
        "Construction",
      ],
    },
);
async function submit() {
  if (busy.value) return;
  busy.value = true;
  feedback.value = "";
  success.value = false;
  try {
    await api.request("/api/public/inquiries", "POST", {
      ...form,
      locale: props.locale,
    });
    success.value = true;
    feedback.value = labels.value.success;
    Object.keys(form).forEach((key) => {
      form[key as keyof typeof form] = "";
    });
  } catch (error) {
    const e = error as { data?: { msg?: string }; statusCode?: number };
    feedback.value =
      props.locale === "zh"
        ? e.data?.msg || labels.value.failure
        : labels.value.failure;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="contact-form-section">
    <div class="contact-intro">
      <p class="eyebrow">GET IN TOUCH</p>
      <h1>{{ labels.title }}</h1>
      <p class="contact-lead">{{ labels.intro }}</p>
      <p class="contact-guidance">{{ labels.guidance }}</p>
      <!-- <div class="contact-guide">
        <h2>{{ labels.stepsTitle }}</h2>
        <ol>
          <li v-for="(step, index) in labels.steps" :key="step">
            <span aria-hidden="true">0{{ index + 1 }}</span>{{ step }}
          </li>
        </ol>
      </div> -->
      <p class="contact-signature">SPECIALTY MATERIALS<br />&amp; APPLICATIONS</p>
    </div>
    <form class="inquiry-form" @submit.prevent="submit">
      <div class="full form-heading">
        <h2>{{ labels.formTitle }}</h2>
        <p>{{ labels.requiredNote }}</p>
      </div>
      <label>{{ labels.name }} *<input v-model="form.name" required maxlength="80" autocomplete="name" /></label>
      <label>{{ labels.email }} *<input v-model="form.email" required type="email" maxlength="180"
          autocomplete="email" /></label>
      <label>{{ labels.country
        }}<select v-model="form.country">
          <option value="">{{ labels.country }}</option>
          <option v-for="(name, i) in labels.countries" :key="i" :value="['VN', 'CN', 'TH', 'MY', 'SG'][i]">
            {{ name }}
          </option>
          <option value="OTHER">{{ labels.other }}</option>
        </select></label>
      <label>{{ labels.phone }} *<input v-model="form.phone" required type="tel" maxlength="40"
          pattern="[+0-9 ()-]{1,40}" autocomplete="tel" /></label>
      <label>{{ labels.industry
        }}<select v-model="form.industry">
          <option value="">{{ labels.industry }}</option>
          <option v-for="(name, i) in labels.industries" :key="i" :value="[
            'PLASTICS',
            'AUTOMOTIVE',
            'PACKAGING',
            'ELECTRONICS',
            'CONSTRUCTION',
          ][i]
            ">
            {{ name }}
          </option>
          <option value="OTHER">{{ labels.other }}</option>
        </select></label>
      <label>{{ labels.company
        }}<input v-model="form.company" maxlength="180" autocomplete="organization" /></label>
      <label class="full">{{ labels.message }} *<textarea v-model="form.message" required maxlength="4000" rows="5"
          :placeholder="labels.messageHint" />
      </label>
      <div class="full form-actions">
        <p class="privacy">{{ labels.privacy }}</p>
        <button class="button" type="submit" :disabled="busy">
          {{ busy ? labels.sending : labels.submit }}
        </button>
      </div>
      <p v-if="feedback" class="full form-feedback" :class="{ success }" role="status">
        {{ feedback }}
      </p>
    </form>
  </section>
</template>

<style scoped>
.contact-form-section {
  display: grid;
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.4fr);
  align-items: start;
  gap: 48px;
}

.contact-intro {
  padding: 32px 0;
}

.contact-form-section h1 {
  font-size: clamp(36px, 4vw, 52px);
  line-height: 1.2;
  letter-spacing: -1px;
  margin-bottom: 24px;
}

.contact-lead {
  font-size: 21px;
  line-height: 1.7;
  max-width: 360px;
  margin-bottom: 16px;
}

.contact-guidance {
  color: var(--muted);
  font-size: 14px;
  max-width: 370px;
}

.contact-guide {
  background: var(--soft);
  border-left: 3px solid var(--green);
  padding: 24px;
  margin-top: 32px;
}

.contact-guide h2 {
  font-size: 16px;
  margin-bottom: 20px;
}

.contact-guide ol {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 18px;
}

.contact-guide li {
  display: flex;
  align-items: baseline;
  gap: 14px;
  font-size: 14px;
  line-height: 1.6;
}

.contact-guide li span {
  color: var(--green);
  font-size: 12px;
  font-weight: 700;
}

.contact-signature {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--muted);
  margin: 32px 0 0;
}

.inquiry-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px 24px;
  padding: 36px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--white);
  box-shadow: 0 12px 40px rgb(37 53 38 / 5%);
  min-width: 0;
}

.form-heading {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.form-heading h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.form-heading p {
  color: var(--muted);
  font-size: 12px;
  margin: 0;
}

label {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
}

input,
select,
textarea {
  width: 100%;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #fafbf8;
  color: var(--ink);
  font: inherit;
  line-height: 1.5;
  transition: border-color 0.2s, background 0.2s;
}

input:hover,
select:hover,
textarea:hover {
  border-color: #aab593;
}

input:focus,
select:focus,
textarea:focus {
  background: var(--white);
  outline: 2px solid var(--green);
  outline-offset: 2px;
}

textarea {
  resize: vertical;
}

textarea::placeholder {
  color: var(--muted);
  font-size: 13px;
}

.full {
  grid-column: 1 / -1;
}

.privacy {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 6px;
}

.form-actions .privacy {
  flex: 1;
}

.form-actions .button {
  flex-shrink: 0;
}

.form-feedback {
  margin: 0;
  padding: 12px 16px;
  background: var(--soft);
  border-left: 3px solid var(--green);
  font-size: 14px;
}

.success {
  color: #466224;
}

button:disabled {
  opacity: 0.6;
  cursor: wait;
}

@media (max-width: 900px) {
  .contact-form-section {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .contact-intro {
    padding: 0;
  }

  .contact-lead,
  .contact-guidance {
    max-width: none;
  }

  .contact-signature {
    display: none;
  }
}

@media (max-width: 640px) {
  .inquiry-form {
    grid-template-columns: 1fr;
    gap: 18px;
    padding: 24px 20px;
  }

  .contact-guide {
    padding: 20px;
    margin-top: 24px;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .form-actions .button {
    width: 100%;
  }
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="版权声明和用户协议"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="copyright-content">
      <el-scrollbar height="500px">
        <div class="content-section">
          <h3>一、知识产权归属</h3>
          <ol>
            <li>您上传的文档的知识产权归您所有</li>
            <li>您授权本平台展示、分享您的文档</li>
            <li>其他用户下载您的文档需要消耗积分</li>
            <li>平台不会将您的文档用于商业用途</li>
          </ol>
        </div>

        <div class="content-section">
          <h3>二、版权保证</h3>
          <p>您保证：</p>
          <ol>
            <li>上传的文档为您原创或已获得合法授权</li>
            <li>文档内容不侵犯他人知识产权</li>
            <li>文档内容不包含违法、违规信息</li>
            <li>文档内容不包含敏感词、暴力、色情等不当内容</li>
            <li>文档内容不包含广告、联系方式等营销信息</li>
          </ol>
        </div>

        <div class="content-section">
          <h3>三、审核机制</h3>
          <p>为保证平台内容质量，我们实行双层审核机制：</p>
          <ol>
            <li>
              <strong>初审（自动化）</strong>
              <ul>
                <li>格式检查：文件大小、扩展名、标题描述</li>
                <li>内容合规性：敏感词过滤、联系方式检测</li>
                <li>相似度分析：防止重复上传</li>
              </ul>
            </li>
            <li>
              <strong>复审（人工审核）</strong>
              <ul>
                <li>学术性评估：文档的学术价值</li>
                <li>原创性评估：内容的原创程度</li>
                <li>实用性评估：对用户的实用价值</li>
                <li>版权合规性：是否存在侵权问题</li>
              </ul>
            </li>
          </ol>
          <p class="note">只有通过双层审核的文档才能发布，并获得积分奖励。</p>
        </div>

        <div class="content-section">
          <h3>四、侵权责任</h3>
          <p>如果您的文档侵犯他人权益：</p>
          <ol>
            <li>平台有权立即删除文档</li>
            <li>您需承担相应法律责任</li>
            <li>严重者将被封禁账号</li>
            <li>已获得的积分将被扣除</li>
          </ol>
        </div>

        <div class="content-section">
          <h3>五、申诉机制</h3>
          <p>如果您对审核结果有异议：</p>
          <ol>
            <li>可以在"我的申诉"页面提交申诉</li>
            <li>需要提供详细的申诉理由和证据材料</li>
            <li>管理员会在3个工作日内处理您的申诉</li>
            <li>申诉通过后，文档将重新进入审核流程</li>
          </ol>
        </div>

        <div class="content-section">
          <h3>六、举报机制</h3>
          <p>如发现侵权文档，请通过以下方式举报：</p>
          <ol>
            <li>在文档详情页点击"举报"按钮</li>
            <li>选择举报类型（版权侵权、抄袭、违法内容等）</li>
            <li>提供详细的举报描述和证据链接</li>
            <li>留下您的联系方式，以便我们与您沟通</li>
          </ol>
          <p class="note">我们会认真对待每一个举报，并在5个工作日内给出处理结果。</p>
        </div>

        <div class="content-section">
          <h3>七、积分规则</h3>
          <ol>
            <li>文档通过复审后，您将获得上传积分奖励</li>
            <li>文档被下载时，您将获得额外积分</li>
            <li>如果文档被删除，已获得的积分将被扣除</li>
            <li>恶意上传、重复上传将不会获得积分</li>
          </ol>
        </div>

        <div class="content-section">
          <h3>八、免责声明</h3>
          <ol>
            <li>平台仅提供文档分享服务，不对文档内容负责</li>
            <li>用户应自行判断文档的准确性和可靠性</li>
            <li>因使用文档造成的任何损失，平台不承担责任</li>
            <li>平台保留随时修改本协议的权利</li>
          </ol>
        </div>

        <div class="content-section highlight">
          <h3>九、特别提示</h3>
          <p class="warning">
            <el-icon><Warning /></el-icon>
            请务必确保您上传的文档不侵犯他人知识产权，否则您将承担全部法律责任！
          </p>
          <p class="warning">
            <el-icon><Warning /></el-icon>
            恶意举报、虚假举报将受到处罚，严重者将被封禁账号！
          </p>
        </div>

        <div class="content-section">
          <p class="agreement-footer">
            本协议自您勾选"我已阅读并同意"并点击"上传文档"按钮时生效。
            <br />
            如有疑问，请联系客服：support@knowledge-platform.com
          </p>
        </div>
      </el-scrollbar>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-checkbox v-model="agreed" size="large">
          我已仔细阅读并同意以上所有条款
        </el-checkbox>
        <div class="footer-buttons">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" :disabled="!agreed" @click="handleConfirm">
            确认并继续
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(props.modelValue)
const agreed = ref(false)

watch(
  () => props.modelValue,
  (newValue) => {
    visible.value = newValue
    if (newValue) {
      agreed.value = false
    }
  }
)

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

const handleCancel = () => {
  visible.value = false
  agreed.value = false
}

const handleConfirm = () => {
  if (agreed.value) {
    emit('confirm')
    visible.value = false
  }
}
</script>

<style scoped lang="scss">
.copyright-content {
  .content-section {
    margin-bottom: 24px;
    padding: 16px;
    background-color: #f5f7fa;
    border-radius: 8px;

    h3 {
      margin: 0 0 12px 0;
      color: #303133;
      font-size: 16px;
      font-weight: 600;
    }

    p {
      margin: 8px 0;
      color: #606266;
      line-height: 1.6;
    }

    ol {
      margin: 8px 0;
      padding-left: 24px;
      color: #606266;

      li {
        margin: 8px 0;
        line-height: 1.6;
      }
    }

    ul {
      margin: 8px 0;
      padding-left: 24px;
      list-style-type: circle;

      li {
        margin: 4px 0;
        line-height: 1.5;
      }
    }

    .note {
      margin-top: 12px;
      padding: 8px 12px;
      background-color: #e6f7ff;
      border-left: 3px solid #1890ff;
      color: #0050b3;
      font-size: 14px;
    }

    &.highlight {
      background-color: #fff7e6;
      border: 1px solid #ffd591;

      .warning {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 12px 0;
        padding: 12px;
        background-color: #fff;
        border-left: 3px solid #fa8c16;
        color: #d46b08;
        font-weight: 500;

        .el-icon {
          font-size: 18px;
        }
      }
    }

    .agreement-footer {
      margin-top: 16px;
      padding: 16px;
      background-color: #fff;
      border: 1px solid #d9d9d9;
      border-radius: 4px;
      text-align: center;
      color: #8c8c8c;
      font-size: 13px;
      line-height: 1.8;
    }
  }
}

.dialog-footer {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .el-checkbox {
    font-weight: 500;
  }

  .footer-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>

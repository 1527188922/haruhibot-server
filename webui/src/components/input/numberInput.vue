<template>
  <el-input
    :maxlength="maxlength"
    v-bind="$attrs"
    :value="value"
    @input="_input"
    @keypress.native="_onKeyPress"
  >
    <template v-if="slotName">
      <span :slot="slotName" v-html="slotContent"></span>
    </template>
  </el-input>
</template>

<script>
export default {
  name: 'numberInput',
  props: {
    value: [Number, String],
    type: {
      type: String,
      default: 'integer' // integer,money
    },
    maxlength: {
      type: [Number, String],
      default: 12
    },
    max:{
      type:Number,
      default: Infinity
    },
    min:{
      type:Number,
      default: -Infinity
    },
    slotName:{
      type:String,
      default:null
    },
    slotContent:{
      type:String,
      default:null
    }
  },
  methods: {
    _input(val) {
      const num_reg = /^[0-9]+[.]?[0-9]{0,2}$/
      let value = num_reg.test(val) ? val : ''
      if (!value) {
        this.$emit('input', ' ')
        this.$nextTick(() => {
          this.$emit('input', value)
        })
      } else {
        this.$emit('input', value)
      }
      if(this.type === 'integer'){
        let number = Number.parseInt(value)
        if(number < this.min){
          this.$emit('input', this.min)
        }
        if(this.max && number > this.max){
          this.$emit('input', this.max)
        }
      }
    },
    _onKeyPress(event) {
      let key = event.key // 输入字符的键值
      let value = event.target.value // 输入框现有的值
      const numberKeySet = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']
      const dotKeySet = ['.', 'Del'] // 'Del'为ie数字小键盘的'.'
      const actionKeySet = ['Backspace']
      // 0 的keyCode是48， 9的keyCode是57，小数点的keyCode为190,回退为8；
      if (!numberKeySet.includes(key) && !dotKeySet.includes(key) && !actionKeySet.includes(key)) {
        // 禁止输入【0~9和.】以外的字符
        event.preventDefault()
      } else if (this.type === 'integer') {
        // 如果要求是整数
        if (dotKeySet.includes(key)) {
          // 禁止输入小数点
          event.preventDefault()
        } else if (value.length === 1 && value === '0') {
          // 如果第一个字符是0，就禁止输入；
          event.preventDefault()
        }
      } else if (this.type === 'money') {
        // 如果要求输入金额，可以保留两位小数
        if (value.length === 0 && !numberKeySet.includes(key)) {
          // 第一位除了数字，禁止输入其他的
          event.preventDefault()
        } else if (
          value.length === 1 &&
          value === '0' &&
          !dotKeySet.includes(key) &&
          !actionKeySet.includes(key)
        ) {
          // 如果第一个字符是0，第二位必须只能输入小数点或者回车
          event.preventDefault()
        } else if (value.includes('.') && dotKeySet.includes(key)) {
          // 如果已经包含小数点，就禁止输入小数点
          event.preventDefault()
        } else if (
          value.split('.')[1] &&
          value.split('.')[1].length === 2 &&
          !actionKeySet.includes(key)
        ) {
          // 如果小数位的长度大于2，则除了回退禁止输入
          event.preventDefault()
        }
      }
    }
  }
}
</script>

<style scoped></style>

<template>
  <div id="index">
    <bot-ws-info-card></bot-ws-info-card>


<!--    <basic-container class="block" shadow="always" :show-header="true" v-loading="botWsInfoLoading">-->
<!--      <template slot="header">-->
<!--        <div class="title">-->
<!--          BOT WebSocket服务-->
<!--        </div>-->
<!--      </template>-->
<!--    </basic-container>-->

  </div>
</template>

<script>

import botWsInfoCard from "./bot-ws-info-card.vue";
export default {
  name: "wel",
  components:{
    botWsInfoCard
  },
  data () {
    return {

    }
  },
  computed: {
  },
  mounted() {

  },
  created () {
  },
  methods: {

  }
};
</script>

<style scoped lang="scss">
#index {
  .block{
    display: inline-block;
    vertical-align: top;
    padding: 0;
    margin: 0 0 10px 15px;
    width: 300px;
    ::v-deep .el-card{
      min-height: 350px;
      .el-card__body{
        min-height: calc(350px - 30px - 60px);
      }
    }
  }
  .title{
    margin-bottom: 0;
    text-align: center;
    font-weight: bold;
    font-size: 15px;
  }
}
</style>

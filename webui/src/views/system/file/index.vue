<template>
  <div id="SystemFile">

    <el-tree
        ref="tree"
        :data="fileNodes"
        :props="props"
        :load="loadNode"
        node-key="absolutePath"
        lazy
        highlight-current
        @node-click="handleNodeClick"
    />
  </div>
</template>
<script>
import {findFileNodes} from "@/api/system";

export default {
  name:'SystemFile',
  data(){
    return{
      fileNodes:[],
      props:{
        label: 'fileName',
        isLeaf: 'leaf'
      }
    }
  },
  created() {

  },
  mounted() {

  },
  methods:{
    async loadNode(node, resolve){
      console.log('loadNode node',node)
      let {data:{data,code}} = await findFileNodes(node.data.absolutePath)
      console.log('loadNode data',data)
      resolve(data)
    },
    handleNodeClick(data, node){
      console.log('handleNodeClick',data,node)
    }
  }
}
</script>
<style lang="scss" scoped>
#SystemFile{

}
</style>
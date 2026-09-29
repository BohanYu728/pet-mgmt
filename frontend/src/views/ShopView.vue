<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import AppPhoto from '../components/AppPhoto.vue'
import { api, errorMessage, imageUrl, unwrap } from '../api'

const props = defineProps({ section: String })
const page = computed(() => props.section || 'products')
const items = ref([]), cart = ref({ items: [], total_amount: 0 }), orders = ref([])
const selected = ref(null), productDetail = ref(null), error = ref(''), message = ref('')
const species = ref(''), category = ref(''), query = ref(''), pageIndex = ref(1), pages = ref(1), quantity = ref(1)
const address = reactive({ recipient_name: '', recipient_phone: '', address_province: '', address_city: '', address_district: '', address_detail: '', note: '' })
const title = computed(() => page.value === 'cart' ? '购物车' : page.value === 'orders' ? '我的订单' : '宠物商城')
const sampleImage = product => product?.category === 'food' ? '/images/product-food.jpg' : product?.category === 'toy' ? '/images/product-toy.jpg' : '/images/product-care.jpg'

async function load() {
  error.value = ''
  try {
    if (page.value === 'products') {
      const data = (await api.get('/shop/products', { params: { page: pageIndex.value, species: species.value || undefined, category: category.value || undefined, q: query.value || undefined } })).data
      items.value = data.items || []; pages.value = data.pages || 1
    } else if (page.value === 'cart') cart.value = (await api.get('/shop/cart')).data
    else if (page.value === 'orders') {
      const data = (await api.get('/shop/orders', { params: { page: pageIndex.value } })).data
      orders.value = data.items || []; pages.value = data.pages || 1
    }
  } catch (e) { error.value = errorMessage(e) }
}
onMounted(load); watch(page, () => { pageIndex.value = 1; message.value = ''; load() })
function filter() { pageIndex.value = 1; load() }
function changePage(next) { pageIndex.value = next; load() }
async function viewProduct(product) { try { productDetail.value = (await api.get(`/shop/products/${product.id}`)).data.product } catch (e) { error.value = errorMessage(e) } }
async function add(product) { error.value = ''; try { unwrap(await api.post('/shop/cart', { product_id: product.id, quantity: Number(quantity.value) })); message.value = `${product.name} 已加入购物车` } catch (e) { error.value = errorMessage(e) } }
async function remove(item) { try { unwrap(await api.delete(`/shop/cart/${item.id}`)); await load() } catch (e) { error.value = errorMessage(e) } }
async function clear() { if (!confirm('确定清空购物车吗？')) return; try { unwrap(await api.post('/shop/cart/clear')); await load() } catch (e) { error.value = errorMessage(e) } }
async function checkout() { error.value = ''; try { unwrap(await api.post('/shop/orders', address)); message.value = '下单成功，可在我的订单中查看进度。'; await load() } catch (e) { error.value = errorMessage(e) } }
async function detail(order) { try { selected.value = (await api.get(`/shop/orders/${order.id}`)).data.order } catch (e) { error.value = errorMessage(e) } }
async function cancel(order) { if (!confirm('确定取消此订单吗？')) return; try { unwrap(await api.put(`/shop/orders/${order.id}/status`, { status: 'cancelled' })); await load() } catch (e) { error.value = errorMessage(e) } }
</script>

<template>
  <section class="shop-page">
    <div class="shop-heading page-head">
      <div><p class="eyebrow"><AppIcon name="shop" :size="15" /> 用心挑选 · 宠物生活</p><h1>{{ title }}</h1><p>{{ page === 'products' ? '为它选择合适的食物、玩具与日常护理。' : page === 'cart' ? '核对心选好物，准备送往毛孩子身边。' : '查看每一份心意的配送进度。' }}</p></div>
      <div v-if="page === 'products'" class="shop-intro"><AppIcon name="heart" :size="20" /><span>适合它的，才是更好的。</span></div>
    </div>
    <nav class="tabs shop-tabs" aria-label="商城导航"><RouterLink to="/shop/products"><AppIcon name="shop" :size="16" /> 商品浏览</RouterLink><RouterLink to="/shop/cart"><AppIcon name="bag" :size="16" /> 购物车</RouterLink><RouterLink to="/shop/orders"><AppIcon name="clock" :size="16" /> 我的订单</RouterLink></nav>
    <div v-if="error" class="message error">{{ error }}</div>
    <div v-if="message" class="message">{{ message }}</div>

    <template v-if="page === 'products'">
      <form class="card shop-filters" @submit.prevent="filter">
        <label class="field query-field"><span>搜索商品</span><div class="input-icon"><AppIcon name="search" :size="17" /><input v-model.trim="query" placeholder="商品名称"></div></label>
        <label class="field"><span>适用宠物</span><select v-model="species" @change="filter"><option value="">全部</option><option value="dog">狗狗</option><option value="cat">猫咪</option></select></label>
        <label class="field"><span>商品分类</span><select v-model="category" @change="filter"><option value="">全部</option><option value="food">宠物食品</option><option value="toy">宠物玩具</option><option value="accessory">用品配件</option><option value="medicine">健康护理</option><option value="other">其他</option></select></label>
        <button class="btn filter-button"><AppIcon name="filter" :size="16" /> 搜索</button>
      </form>
      <div class="catalog-meta"><span>{{ items.length ? `本页 ${items.length} 件商品` : '商品目录' }}</span><span v-if="pages > 1">第 {{ pageIndex }} / {{ pages }} 页</span></div>
      <div v-if="items.length" class="product-grid grid">
        <article v-for="product in items" :key="product.id" class="product-card card">
          <div class="product-media"><AppPhoto :src="product.image ? imageUrl(product.image) : ''" :fallback="sampleImage(product)" fallback-alt="商品分类示意照片" :alt="product.image ? product.name : `${product.category_display}分类示意照片`" /><span class="product-species">{{ product.target_species_display }}</span></div>
          <div class="product-content"><p class="product-category">{{ product.category_display }}</p><h2>{{ product.name }}</h2><p class="product-description">{{ product.description || '查看详情了解商品信息。' }}</p><div class="product-bottom"><strong>¥{{ Number(product.price).toFixed(2) }}</strong><span :class="product.stock < 1 ? 'out-of-stock' : ''">{{ product.stock < 1 ? '暂时缺货' : `库存 ${product.stock}` }}</span></div><div class="product-actions actions"><button class="btn secondary" @click="viewProduct(product)">查看详情</button><input v-model.number="quantity" aria-label="购买数量" class="search-input number-input" type="number" min="1" :max="product.stock"><button class="btn" :disabled="product.stock < 1" @click="add(product)">加入购物车</button></div></div>
        </article>
      </div>
      <div v-else class="empty"><AppIcon name="search" :size="34" /><h2>没有找到匹配的商品</h2><p>试试更短的关键词或切换分类。</p></div>
    </template>

    <template v-else-if="page === 'cart'">
      <div v-if="cart.items?.length" class="cart-layout two-col">
        <div class="card cart-items"><h2>已选商品 <small>{{ cart.item_count }} 件</small></h2><div v-for="item in cart.items" :key="item.id" class="list-row cart-row"><div class="cart-product"><div class="cart-thumb"><AppPhoto :src="item.product?.image ? imageUrl(item.product.image) : ''" :fallback="sampleImage(item.product)" fallback-alt="商品分类示意照片" compact :alt="item.product?.name || '商品分类示意照片'" /></div><div><strong>{{ item.product?.name || '商品' }}</strong><div class="muted small">¥{{ item.product?.price }} × {{ item.quantity }}</div></div></div><button class="btn danger" @click="remove(item)">移除</button></div><div class="cart-total"><span>商品合计</span><strong>¥{{ Number(cart.total_amount).toFixed(2) }}</strong></div><button class="text-button clear-cart" @click="clear">清空购物车</button></div>
        <form class="card checkout-card" @submit.prevent="checkout"><div class="checkout-intro"><AppIcon name="bag" :size="23" /><div><h2>收货信息</h2><p>确认地址后提交订单</p></div></div><div class="form-grid"><label class="field"><span>收件人</span><input v-model="address.recipient_name" required></label><label class="field"><span>联系电话</span><input v-model="address.recipient_phone" required type="tel"></label><label class="field"><span>省份</span><input v-model="address.address_province" required></label><label class="field"><span>城市</span><input v-model="address.address_city" required></label><label class="field"><span>区县</span><input v-model="address.address_district"></label><label class="field full"><span>详细地址</span><input v-model="address.address_detail" required></label><label class="field full"><span>订单备注</span><textarea v-model="address.note"></textarea></label></div><button class="btn checkout-button">确认下单 <AppIcon name="arrow" :size="16" /></button></form>
      </div>
      <div v-else class="empty"><AppIcon name="bag" :size="38" /><h2>购物车还是空的</h2><p>去看看哪些好物适合毛孩子。</p><RouterLink class="btn" to="/shop/products">挑选商品</RouterLink></div>
    </template>

    <template v-else>
      <div v-if="orders.length" class="card order-list"><div v-for="order in orders" :key="order.id" class="list-row order-row"><div><span class="order-number">订单 #{{ order.id }}</span><div class="muted small">{{ order.created_at?.slice(0,16) }} · {{ order.item_count }} 件商品</div><strong class="order-price">¥{{ Number(order.total_amount).toFixed(2) }}</strong></div><div class="actions"><span class="tag" :class="`status-${order.status}`">{{ order.status_display }}</span><button class="btn secondary" @click="detail(order)">查看详情</button><button v-if="order.status === 'pending'" class="btn danger" @click="cancel(order)">取消订单</button></div></div></div>
      <div v-else class="empty"><AppIcon name="bag" :size="38" /><h2>还没有订单</h2><p>挑选第一件送给毛孩子的礼物。</p><RouterLink class="btn" to="/shop/products">开始选购</RouterLink></div>
    </template>

    <div v-if="pages > 1 && page !== 'cart'" class="actions pagination"><button class="btn secondary" :disabled="pageIndex<=1" @click="changePage(pageIndex-1)">上一页</button><span>第 {{ pageIndex }} / {{ pages }} 页</span><button class="btn secondary" :disabled="pageIndex>=pages" @click="changePage(pageIndex+1)">下一页</button></div>

    <div v-if="productDetail" class="modal-backdrop" @click.self="productDetail=null"><div class="modal detail-modal"><div class="page-head"><h2>{{ productDetail.name }}</h2><button class="text-button" @click="productDetail=null">关闭 ✕</button></div><div class="detail-layout"><div class="detail-photo"><AppPhoto :src="productDetail.image ? imageUrl(productDetail.image) : ''" :fallback="sampleImage(productDetail)" fallback-alt="商品分类示意照片" :alt="productDetail.image ? productDetail.name : '商品分类示意照片'" /></div><div><p class="product-category">{{ productDetail.category_display }} · {{ productDetail.target_species_display }}</p><p class="detail-description">{{ productDetail.description }}</p><strong class="detail-price">¥{{ Number(productDetail.price).toFixed(2) }}</strong><p class="muted small">库存 {{ productDetail.stock }}</p><div class="actions"><button class="btn" :disabled="productDetail.stock<1" @click="add(productDetail);productDetail=null">加入购物车</button></div></div></div></div></div>
    <div v-if="selected" class="modal-backdrop" @click.self="selected=null"><div class="modal order-modal"><div class="page-head"><h2>订单 #{{ selected.id }}</h2><button class="text-button" @click="selected=null">关闭 ✕</button></div><p><span class="tag">{{ selected.status_display }}</span> · 合计 ¥{{ selected.total_amount }}</p><p>收件人：{{ selected.recipient_name }} {{ selected.recipient_phone }}</p><p>地址：{{ selected.address_full }}</p><div v-for="item in selected.items" :key="item.id" class="list-row"><span>{{ item.product_name }} × {{ item.quantity }}</span><strong>¥{{ item.subtotal }}</strong></div></div></div>
  </section>
</template>

<style scoped>
.shop-page{max-width:1280px;margin:0 auto}.shop-heading{align-items:flex-end}.shop-heading h1{font-size:clamp(28px,3vw,38px)}.shop-heading p:not(.eyebrow){margin-top:9px}.eyebrow{display:flex;align-items:center;gap:7px;color:var(--brand,#E87832);font-size:12px;font-weight:800;letter-spacing:.12em;margin:0 0 9px}.shop-intro{color:var(--accent,#2A7E86);font-size:13px;font-weight:700;display:flex;align-items:center;gap:9px;border-left:1px solid var(--line,#e0e8ef);padding:12px 0 12px 18px}.shop-tabs{margin:23px 0}.shop-tabs a{display:inline-flex;align-items:center;gap:8px}.shop-filters{display:grid;grid-template-columns:minmax(220px,2fr) repeat(2,minmax(140px,1fr)) auto;align-items:end;gap:15px;margin-bottom:15px;padding:19px}.shop-filters .field{min-width:0}.input-icon{position:relative}.input-icon :deep(svg){position:absolute;left:12px;top:50%;transform:translateY(-50%);color:var(--accent,#2A7E86)}.input-icon input{width:100%;padding-left:38px}.filter-button{height:43px}.catalog-meta{display:flex;justify-content:space-between;color:var(--muted,#6b7b90);font-size:12px;margin:12px 2px 15px}.product-grid{grid-template-columns:repeat(auto-fill,minmax(245px,1fr));gap:19px}.product-card{padding:0;overflow:hidden;border:1px solid var(--line,#e0e8ef);box-shadow:0 12px 32px #263a5010;transition:transform .2s,box-shadow .2s}.product-card:hover{transform:translateY(-3px);box-shadow:0 19px 38px #263a501b}.product-media{height:196px;background:#edf4f7;position:relative}.product-media :deep(img),.detail-photo :deep(img),.cart-thumb :deep(img){width:100%;height:100%;object-fit:cover;display:block}.sample-label,.product-species{position:absolute;top:13px;border-radius:999px;padding:5px 9px;font-size:11px;font-weight:700;background:#ffffffdf;color:var(--ink,#253247);backdrop-filter:blur(8px)}.sample-label{right:12px}.product-species{left:12px}.product-content{padding:19px}.product-category{color:var(--accent,#2A7E86);font-size:11px;font-weight:800;letter-spacing:.08em;margin:0 0 7px}.product-content h2{font-size:17px;color:var(--ink,#253247);margin:0;line-height:1.45;min-height:49px}.product-description{color:var(--muted,#6b7b90);font-size:12px;line-height:1.55;min-height:37px;max-height:37px;overflow:hidden;margin:9px 0 16px}.product-bottom{display:flex;align-items:baseline;justify-content:space-between;border-top:1px solid var(--line,#e0e8ef);padding-top:13px}.product-bottom strong,.order-price,.detail-price{color:var(--brand,#E87832);font-size:21px;font-weight:800;font-variant-numeric:tabular-nums}.product-bottom span{font-size:12px;color:var(--muted,#6b7b90)}.product-bottom .out-of-stock{color:#b35757}.product-actions{gap:6px;margin-top:15px}.product-actions .btn{padding:8px 10px;font-size:12px}.product-actions .number-input{width:52px!important;padding:8px 5px;text-align:center}.product-actions .btn:last-child{flex:1}.cart-layout{grid-template-columns:minmax(0,1.15fr) minmax(360px,.85fr);align-items:start}.cart-items h2,.checkout-card h2{color:var(--ink,#253247);font-size:20px;margin:0 0 15px}.cart-items h2 small{color:var(--muted,#6b7b90);font-size:12px;font-weight:500;margin-left:5px}.cart-row{align-items:center}.cart-product{display:flex;align-items:center;gap:13px;min-width:0}.cart-product strong{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.cart-thumb{width:66px;height:66px;flex:0 0 66px;border-radius:9px;overflow:hidden;background:#eaf3f8}.cart-total{display:flex;justify-content:space-between;align-items:baseline;padding:18px 0;border-top:1px solid var(--line,#e0e8ef)}.cart-total strong{color:var(--brand,#E87832);font-size:24px}.clear-cart{color:var(--muted,#6b7b90);font-size:12px}.checkout-intro{display:flex;gap:12px;align-items:start;margin-bottom:21px;color:var(--accent,#2A7E86)}.checkout-intro h2{margin:0}.checkout-intro p{font-size:12px;color:var(--muted,#6b7b90);margin:4px 0 0}.checkout-button{margin-top:18px;width:100%;justify-content:space-between}.order-list{padding:6px 23px}.order-row{padding:19px 0}.order-number{font-size:17px;font-weight:800;color:var(--ink,#253247)}.order-price{display:inline-block;font-size:17px;margin-top:7px}.order-row .actions{align-items:center}.tag.status-cancelled{background:#edf0f3;color:#667789}.tag.status-pending{background:#fff1e6;color:#ae5d29}.tag.status-delivered{background:#e7f4f1;color:#267972}.pagination{justify-content:center;align-items:center;margin-top:25px;color:var(--muted,#6b7b90);font-size:13px}.empty h2{margin:12px 0 5px;color:var(--ink,#253247);font-size:20px}.empty p{margin:0 0 17px}.empty :deep(svg){color:var(--accent,#2A7E86)}.detail-modal{max-width:760px}.detail-layout{display:grid;grid-template-columns:1fr 1fr;gap:23px}.detail-photo{position:relative;height:285px;border-radius:12px;overflow:hidden;background:#eaf3f8}.detail-description{line-height:1.7;white-space:pre-wrap}.detail-price{font-size:28px}.order-modal{max-width:600px}@media(max-width:980px){.shop-filters{grid-template-columns:repeat(2,minmax(0,1fr))}.query-field{grid-column:1/-1}.filter-button{height:42px}.cart-layout{grid-template-columns:1fr}}@media(max-width:600px){.shop-intro{display:none}.shop-filters{grid-template-columns:1fr}.query-field{grid-column:auto}.product-grid{grid-template-columns:1fr}.detail-layout{grid-template-columns:1fr}.detail-photo{height:220px}.order-row{align-items:flex-start;flex-direction:column}.order-row .actions{margin-top:10px}}@media(prefers-reduced-motion:reduce){.product-card{transition:none}.product-card:hover{transform:none}}
</style>
<style scoped>
.product-media :deep(.photo-frame),.detail-photo :deep(.photo-frame),.cart-thumb :deep(.photo-frame){height:100%}
</style>

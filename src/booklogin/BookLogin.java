/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package booklogin;

import java.io.File;
import java.io.IOException;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.SayCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Administrator
 */
public class BookLogin extends JavaPlugin{
    int 登录超时时间=60;
    Plugin 插件;
    ItemStack 登录书;
    YamlConfiguration 配置文件;
    Map<String, YamlConfiguration> 玩家信息缓存=new HashMap<>();
    Map<String, List<ItemStack>> 玩家背包=new HashMap<>();
    Map<String, byte[]> 玩家登陆IP=new HashMap<>();
    PotionEffect 减速效果=new PotionEffect(PotionEffectType.SLOW, 2147483647, 10, false);
    PotionEffect 过重效果=new PotionEffect(PotionEffectType.JUMP, 2147483647, -10, false);
    PotionEffect 残废效果=new PotionEffect(PotionEffectType.SLOW_DIGGING, 2147483647, 10, false);
    PotionEffect 失明效果=new PotionEffect(PotionEffectType.BLINDNESS, 2147483647, 10, false);
    public class 玩家登录信息{
        byte IP地址[];
        Player 玩家;
        char 密码[];
        YamlConfiguration 储存文件;
        private 玩家登录信息(Player 登录玩家) {
            玩家=登录玩家;
            File 文件=new File(getDataFolder(),"PlayerData/"+玩家.getName()+".dat");
            IP地址=玩家.getAddress().getAddress().getAddress();
            if(!文件.exists()){
                try {
                    文件.createNewFile();
                } catch (IOException e) {
                    //out.print("创建玩家数据文件失败!");
                }
                储存文件=YamlConfiguration.loadConfiguration(文件);
            }
            else{
            储存文件=YamlConfiguration.loadConfiguration(文件);
            密码=(char[])储存文件.get("密码:");
            }
        }
    }
    public class 玩家登录时限 extends Thread{     
            private final Player 玩家;
            private final Plugin plu;
            private 玩家登录时限(Plugin 插件,Player 触发玩家){
                玩家=触发玩家;
                plu=插件;
            }
            public synchronized void 开始计时() {
                super.start(); //To change body of generated methods, choose Tools | Templates.
            }
            @Override
            public void run() {
                int Timeout=登录超时时间;
                while(Timeout!=0)
                {
                    Timeout--;
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BookLogin.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(!玩家.isOnline())
                    {
                        ////out.print("玩家已经退出");
                        return;
                    }
                    if(玩家.getNoDamageTicks()<100000)
                        return;
                }
                getServer().getScheduler().scheduleSyncDelayedTask(plu, new Runnable() {

                    @Override
                    public void run() {
                        玩家.kickPlayer("登录超时");
                    }
                },1);
            }
            
        }
    public void 保存玩家背包(Player 玩家){
        //out.print("开始保存玩家");
        YamlConfiguration 信息=玩家信息缓存.get(玩家.getName());
        int i=0;
        //out.print("开始循环");
        for(ItemStack 物品:玩家.getInventory().getContents()){
            信息.set("物品列表."+i+"", 物品);
            i++;
        }
        //out.print("物品保存完成");
        try {
            信息.save(new File(getDataFolder(),"PlayerData/"+玩家.getName()+".dat"));    
        } catch (IOException e) {
            //out.print("储存玩家背包错误!");
        }
        
    }
    public void 加载玩家背包(Player 玩家){
        //out.print("开始加载玩家");
        YamlConfiguration 信息=玩家信息缓存.get(玩家.getName());
        ItemStack 物品;
         List<ItemStack> 物品列表=new ArrayList<>();
        //out.print("开始循环");
        for(int i=0;i<36;i++){
            物品=信息.getItemStack("物品列表."+i+"");
            //out.print(物品);
            if(物品!=null)
                物品列表.add(物品.clone());
            else
                物品列表.add(物品);
        }
        玩家背包.put(玩家.getName(), 物品列表);
        //out.print("物品加载完成");
    }
    public void 设置初始效果(Player 玩家){
        玩家.addPotionEffect(减速效果);
        玩家.addPotionEffect(过重效果);
        玩家.addPotionEffect(残废效果);
        玩家.addPotionEffect(失明效果);
        玩家.setNoDamageTicks(1000000);
        玩家.getInventory().clear();
        for(int j=0;j<9;j++){
            玩家.getInventory().setItem(j, 登录书);
        }
    }
    public class 事件侦听器 implements Listener{
        @EventHandler
        public void 玩家登陆检测(PlayerJoinEvent 事件){
            Player 玩家=事件.getPlayer();
            玩家登录时限 限制=new 玩家登录时限(插件,玩家);
            YamlConfiguration 信息;
            byte[] IP=玩家.getAddress().getAddress().getAddress();
            //out.print(玩家登陆IP);
            if(玩家登陆IP.get(事件.getPlayer().getName())==null){
                File 文件=new File(getDataFolder(),"PlayerData/"+玩家.getName()+".dat");
                if(!文件.exists()){
                    try {
                        文件.createNewFile();
                    } catch (IOException e) {
                        //out.print("创建玩家数据文件失败!");
                    }
                }
                信息=YamlConfiguration.loadConfiguration(文件);
                玩家信息缓存.put(玩家.getName(), 信息);
                //out.print("新建缓存");
            }
            else{
                byte[] saveip=玩家登陆IP.get(玩家.getName());
                if(saveip!=null)
                {
                    //out.print("第二次登陆");
                    //out.print(Arrays.toString(saveip));
                    //out.print(Arrays.toString(IP));
                    if(Arrays.equals(IP, saveip))
                        return;
                }
                    
            }
            ItemStack 前排物品;
            for(int i=0;i<9;i++){
                前排物品=玩家.getInventory().getItem(i);
                if(前排物品!=null)
                {
                    //out.print(前排物品);
                    if(前排物品.equals(登录书))
                    {
                        //out.print("意外登录中崩溃！");
                        加载玩家背包(玩家);
                        设置初始效果(玩家);
                        限制.开始计时();
                        return;
                    }
                }
            }
            //out.print("开始保存玩家背包");
            PlayerInventory 背包=玩家.getInventory();
            List<ItemStack> 物品列表=new ArrayList<>();
            for(ItemStack 物品:背包.getContents()){
                if(物品!=null)
                    物品列表.add(物品.clone());
                else
                    物品列表.add(物品);
            }
            //物品列表.addAll(Arrays.asList(背包.getContents()));
            //out.print("保存完毕");
            玩家背包.put(玩家.getName(), 物品列表);
            设置初始效果(玩家);
            限制.开始计时();
        }
        @EventHandler
        public void 玩家输入完成(PlayerEditBookEvent 事件){
            if(事件.getPlayer().getNoDamageTicks()<100000)
               return;
            //out.print("输入完成");
            Player 玩家=事件.getPlayer();
            YamlConfiguration 信息=玩家信息缓存.get(事件.getPlayer().getName());
            //out.print("获取信息");
            //out.print(事件.getNewBookMeta().toString());
            BookMeta 书本=事件.getNewBookMeta();
            if(信息.getString("密码")!=null)
            {
                //out.print("尝试匹配密码");
                //out.print(信息.getString("密码"));
                if(书本.getTitle()==null)
                {
                    玩家.sendMessage("§c密码错误");
                    事件.setCancelled(true);
                    return;
                }
                if(!书本.getTitle().equals(信息.getString("密码")))
                {
                    //out.print("匹配密码失败 退出");
                    玩家.sendMessage("§c密码错误");
                    事件.setCancelled(true);
                    return;
                }
            }
            else{
                //out.print("尝试保存密码");
                //out.print(书本.getTitle());
                信息.set("密码", 书本.getTitle());
                try {
                    信息.save(new File(getDataFolder(),"PlayerData/"+玩家.getName()+".dat"));
                } catch (IOException e) {
                    //out.print("储存失败");
                }

                //out.print("保存密码完成");
            }
            //out.print("开始移除状态");
            玩家.removePotionEffect(PotionEffectType.SLOW);
            玩家.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            玩家.removePotionEffect(PotionEffectType.BLINDNESS);
            玩家.removePotionEffect(PotionEffectType.JUMP);
            玩家.setNoDamageTicks(0);
            玩家.sendMessage("§6登录成功");
            玩家.getInventory().clear();
            List<ItemStack> 背包=玩家背包.get(玩家.getName());
            //out.print(背包.size());
            for(int i=0;i<背包.size();i++){
                玩家.getInventory().setItem(i,背包.get(i));
            }
            玩家背包.remove(玩家.getName());
            玩家登陆IP.put(玩家.getName(), 玩家.getAddress().getAddress().getAddress());
            玩家.setNoDamageTicks(100);
        }
        @EventHandler
        public void 玩家退出游戏(PlayerQuitEvent 事件){
            Player 玩家=事件.getPlayer();
            if(玩家.getNoDamageTicks()>100000)
            {
                List<ItemStack> 背包=玩家背包.get(玩家.getName());
                for(int i=0;i<背包.size();i++){
                    玩家.getInventory().setItem(i,背包.get(i));
                }
            }
            PlayerInventory 背包=玩家.getInventory();
            List<ItemStack> 物品列表=new ArrayList<>();
            for(ItemStack 物品:背包.getContents()){
                if(物品!=null)
                    物品列表.add(物品.clone());
                else
                    物品列表.add(物品);
            }
            玩家背包.put(玩家.getName(), 物品列表);
            保存玩家背包(玩家);
        }
        @EventHandler
        public void 玩家操作事件(PlayerInteractEvent 事件){
            if(事件.getPlayer().getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 玩家丢弃事件(PlayerDropItemEvent 事件){
            if(事件.getPlayer().getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 玩家捡起事件(PlayerPickupItemEvent 事件){
            if(事件.getPlayer().getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 玩家操作物品栏事件(InventoryClickEvent 事件){
           if(事件.getWhoClicked().getNoDamageTicks()<100000)
                return;
           事件.setCancelled(true);
           事件.getView().close();
        }
        @EventHandler
        public void 玩家受到伤害事件(EntityDamageEvent 事件){
            if(!(事件.getEntity() instanceof Player))
                return;
            if(((Player)(事件.getEntity())).getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 玩家说话(AsyncPlayerChatEvent 事件){
            if(事件.getPlayer().getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 玩家重生(PlayerRespawnEvent 事件){
            Player 玩家=事件.getPlayer();
            if(玩家.getNoDamageTicks()<100000)
                return;
            玩家.addPotionEffect(减速效果);
            玩家.addPotionEffect(过重效果);
            玩家.addPotionEffect(残废效果);
            玩家.addPotionEffect(失明效果);
            玩家.setNoDamageTicks(1000000);
        }
        @EventHandler
        public void 玩家输入命令(PlayerCommandPreprocessEvent 事件){
            Player 玩家=事件.getPlayer();
            if(玩家.getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
        @EventHandler
        public void 怪物攻击玩家(EntityTargetEvent 事件){
            if(!(事件.getTarget() instanceof Player))
                return;
            Player 玩家=(Player)事件.getTarget();
            if(玩家.getNoDamageTicks()<100000)
                return;
            事件.setCancelled(true);
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
    public void InitBookLogin(){
        //out.print("初始化BookLogin");
        File 配置文件数据=new File(getDataFolder(),"config.yml");
        File 玩家文件夹=new File(getDataFolder(),"PlayerData/");
        ItemStack 默认登录书=new ItemStack(Material.BOOK_AND_QUILL);
        BookMeta 默认物品属性=(BookMeta)默认登录书.getItemMeta();
        默认物品属性.setDisplayName("§r§6登陆书");
        默认物品属性.addPage("在标题处输入密码");
        默认登录书.setItemMeta(默认物品属性);
        //out.print("初始化默认书");
        if(!玩家文件夹.exists()){
            try {
                玩家文件夹.mkdirs();
            } catch (Exception e) {
                //out.print("创建目录错误");
            }
        }
        if(!配置文件数据.exists())
        {
            //out.print("创建配置文件");
            登录书=默认登录书;
            try {
                配置文件数据.createNewFile();
            } catch (IOException e) {
                //out.print(e);
                //out.print("创建配置文件失败");
            }
            配置文件=YamlConfiguration.loadConfiguration(配置文件数据);
            配置文件.set("TimeOut", 60);
            配置文件.set("book", 默认登录书);
            try {
                配置文件.save(配置文件数据);
            } catch (IOException e) {
                //out.print(e);
                //out.print("储存配置文件失败");
            }
        }
        else
        {
            //out.print("读取配置文件");
            配置文件=YamlConfiguration.loadConfiguration(配置文件数据);
            登录超时时间=配置文件.getInt("TimeOut");
            登录书=配置文件.getItemStack("book");
            if(登录书==null){
                登录书=默认登录书;
            }
        }
        /*File f=new File(getDataFolder(),"PlayerData/1.yml");
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (IOException e) {
                //out.print("error");
            }
        }
        YamlConfiguration tc=YamlConfiguration.loadConfiguration(f);
        tc.addDefault("File", 登录书);
        tc.set("File", 登录书);
        tc.set("测试", 登录书);
        try {
            tc.save(f);
        } catch (IOException e) {
            //out.print("error");
        }*/

    }

    @Override
    public boolean onCommand(CommandSender 命令发送者, Command 命令, String label, String[] 参数列表) {
        if(!命令发送者.hasPermission("op")){
            return false;
        }
        if(参数列表.length==0)
        {
            命令发送者.sendMessage("---------------------BookLogin----------------------");
            命令发送者.sendMessage("[BookLogin] /BookLogin reload - 重载插件");
            命令发送者.sendMessage("[BookLogin] /BookLogin reset  - 清空玩家密码");
            命令发送者.sendMessage("[BookLogin] /BookLogin book   - 更新登录书内容(手持)");
            命令发送者.sendMessage("----------------------------------------------------");
            return true;
        }
        switch(参数列表[0]){
            case "reload":
                InitBookLogin();
                命令发送者.sendMessage("[BookLogin] 重载完毕");
                break;
            case "reset":
                if(参数列表.length==1)
                {
                    命令发送者.sendMessage("[BookLogin] 请输入玩家名称");
                }
                else
                {
                    File 玩家文件=new File(getDataFolder(),"PlayerData/"+参数列表[1]+".dat");
                    if(!玩家文件.exists())
                    {
                        命令发送者.sendMessage("[BookLogin] 玩家不存在");
                    }
                    else
                    {
                        YamlConfiguration 玩家信息=YamlConfiguration.loadConfiguration(玩家文件);
                        玩家信息.set("密码", null);
                        命令发送者.sendMessage("[BookLogin] 重置成功");
                    }
                }
                break;
            case "book":
                if(!(命令发送者 instanceof Player))
                {
                    命令发送者.sendMessage("该功能只能由玩家执行");
                    return false;
                }
                String 书名称="§r§6登录书(编辑状态)";
                Player 玩家=(Player)命令发送者;
                ItemStack 玩家手中物品=玩家.getItemInHand();
                ItemStack 编辑书=new ItemStack(Material.BOOK_AND_QUILL);
                BookMeta 书属性=(BookMeta)编辑书.getItemMeta();
                书属性.setDisplayName(书名称);
                书属性.addPage("内容从第二页开始！\n标题:"+登录书.getItemMeta().getDisplayName());
                for(String s:((BookMeta)登录书.getItemMeta()).getPages())
                {
                    书属性.addPage(s);
                }
                编辑书.setItemMeta(书属性);
                if(玩家手中物品==null)
                {
                    玩家.getInventory().addItem(编辑书);
                    return false;
                }
                if(玩家.getItemInHand().getType()!=Material.BOOK_AND_QUILL)
                {
                    玩家.getInventory().addItem(编辑书);
                    return false;
                }
                if(玩家手中物品.getItemMeta()==null)
                {
                    玩家.getInventory().addItem(编辑书);
                    return false;
                }
                if(!玩家手中物品.getItemMeta().getDisplayName().equalsIgnoreCase(书名称))
                {
                    玩家.getInventory().addItem(编辑书);
                    return false;
                }
                ItemStack 新书=玩家手中物品;
                书属性=(BookMeta)新书.getItemMeta();
                String 新书名称=书属性.getPage(1);
                int pos=新书名称.indexOf("标题:");
                书属性.setDisplayName(新书名称.substring(pos+3));
                //out.print(新书名称.substring(pos+3));
                //out.print(pos);
                int i;
                for(i=0;i<书属性.getPageCount()-1;i++)
                    书属性.setPage(i+1, 书属性.getPage(i+2));
                书属性.setPage(i+1, null);
                新书.setItemMeta(书属性);
                登录书=新书.clone();
                配置文件.set("book", 登录书);
                try {
                    配置文件.save(new File(getDataFolder(),"config.yml"));
                } catch (IOException e) {
                    //out.print(e);
                    //out.print("储存配置文件失败");
                }
                玩家.sendMessage("[BookLogin] 设置成功");
                玩家手中物品.setType(Material.AIR);
                玩家.setItemInHand(玩家手中物品);
                break;
                default:
                    命令发送者.sendMessage("---------------------BookLogin----------------------");
                    命令发送者.sendMessage("[BookLogin] /BookLogin reload - 重载插件");
                    命令发送者.sendMessage("[BookLogin] /BookLogin reset  - 清空玩家密码");
                    命令发送者.sendMessage("[BookLogin] /BookLogin book   - 更新登录书内容(手持)");
                    命令发送者.sendMessage("----------------------------------------------------");
                    break;
        }
        return false;
    }
    

    @Override
    public void onEnable() {
        插件=this;
        getServer().getPluginManager().registerEvents(new 事件侦听器(), this);
        InitBookLogin();
        out.print("登陆书插件已加载!");
    }
    
    
    
}

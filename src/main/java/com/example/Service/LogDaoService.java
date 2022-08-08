package com.example.Service;


import com.example.Dao.LogDao;
import com.example.ES.LogES;
import com.example.Pojo.Log;
import com.example.Run.Email;
import com.example.Run.EmailProperties;
import com.example.Run.EsTemplate;
import com.example.Run.Redis;
import com.example.Utils.Maputil;
import com.example.Utils.Response;
import com.example.Utils.SearchArgs;
import com.example.Utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LogDaoService {
    private final Logger mylog = LoggerFactory.getLogger(LogDaoService.class);
    private final LogDao logDevelopDao;
    private final EsTemplate esTemplate;
    private final LogES logES;
    private final Redis redis;
    private final Email email;
    private final EmailProperties emailProperties;


    @Autowired(required = false)
    public LogDaoService(LogDao logDevelopDao,
                         EsTemplate esTemplate,
                         EmailProperties emailProperties,
                         LogES logES,
                         Redis redis,
                         Email email){
        this.logDevelopDao = logDevelopDao;
        this.esTemplate = esTemplate;
        this.emailProperties = emailProperties;
        this.logES = logES;
        this.redis = redis;
        this.email = email;
    }


    /**
     * 写入到数据库和es中
     * @param logOperation
     */
    public void InsertDB(Log logOperation){
        if (logOperation != null){
            try {
               synchronized (this){
                   this.logDevelopDao.Insertlog(logOperation);
                   Log logOperation1 = this.logDevelopDao.SelectByid(this.logDevelopDao.Maxid());
                   this.logES.save(logOperation1);
               }
            }catch (Exception e){
                e.printStackTrace();
                this.redis.InsertFail(logOperation);
            }
        }
    }

    public Response<List<Log>>findall(PageRequest request){
        SearchHits<Log> searchHits = this.esTemplate.SearchAll(request, Log.class);
        return this.Parse(searchHits);
    }

    public Response<List<Log>> SearchTrem(String filed,  String rule , int size,int page,List<String> value) throws ParseException {
        int num = value.size();
        if (num > 2) return null;

        if (!filed.equals("recorddate"))
        {
            if (num == 1){
                String Newfiled = Maputil.ReplaceAddKeyword(filed);
                String values = value.get(0);
                return this.Parse(this.esTemplate.SearchTerm(Newfiled, values, size, page, Log.class));
            }
        }

        long parselong = 0L;
        long parselongend = 0L;
        if (num == 1) {
            parselong = TimeUtils.Parselong(value.get(0));
        }
        if (num == 2) {
            parselong = TimeUtils.Parselong(value.get(0));
            parselongend = TimeUtils.Parselong(value.get(1));
        }
        PageRequest of = PageRequest.of(0, size);
        List<Log> logs = new ArrayList<>();

        SearchHits<Log> searchHits = this.esTemplate.SearchRange(filed, parselong, parselongend, rule, size, page, Log.class);
        Response<List<Log>> parse = this.Parse(searchHits);
        return parse;
    }

    public Response<List<Log>> Searchlike(String filed, String rule, int size,int page,List<String> value) throws ParseException {
        String values = "";
        int num = value.size();
        if (num == 1) {
            values = value.get(0);
            SearchHits searchHits = this.esTemplate.SearchLike(filed, values, size,page, Log.class);
            return this.Parse(searchHits);
        }
 
        long parselong = 0L;
        long parselongend = 0L;
        if (num == 2) {
            parselong = TimeUtils.Parselong(value.get(0));
            parselongend = TimeUtils.Parselong(value.get(1));
        }
        if (filed.equals("recorddate")) {
            SearchHits<Log> searchHits = this.esTemplate.SearchRange(filed, parselong, parselongend, rule, size, page, Log.class);
            Response<List<Log>> parse = this.Parse(searchHits);
            return parse;
        }
        return new Response<>(null);
    }

    public Response<List<Log>> SearchlikeMutil(Map<String,Object> maps, int size,int page){
        SearchHits<Log> searchHits = this.esTemplate.SearchLikeMutil2(maps, size, page,Log.class);
        return this.Parse(searchHits);
    }

    /**
     * 发送邮件
     */
    public void Emails() {
        String[] objects = (String[]) emailProperties.getTo().toArray();
        email.SetPerson(emailProperties.getFrom(),objects);
        Map<String, String> maps = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String time = dateFormat.format(new Date());
        maps.put("msg", "Brother");
        maps.put("link", "https://baidu.com");
        maps.put("time", time);

        email.SetContent("系统异常", true,
                email.ReplaceParams("SystemError.html", (maps)));
        email.Send();
    }



    public Response<List<Log>> SearchMutilLog(SearchArgs.ArgsItem argsItem,SearchArgs.Order order,int per_page,int curr_page) throws ParseException {
        if (argsItem == null) return null;

        SearchHits<Log> searchHits = this.esTemplate.SearchLikeMutil3(argsItem, order, per_page, curr_page, Log.class);
        Response<List<Log>> parse = this.Parse(searchHits);
        return parse;
    }

    private Response<List<Log>> Parse(SearchHits<Log> searchHits){
        List<SearchHit<Log>> searchHits1 = searchHits.getSearchHits();
        long totalHits = searchHits.getTotalHits();
        List<Log> datas = new ArrayList<>();
        for (SearchHit<Log> comptrollerSearchHit : searchHits1) {
            datas.add(comptrollerSearchHit.getContent());
        }
        return new Response<>(datas, Math.toIntExact(totalHits));
    }




}

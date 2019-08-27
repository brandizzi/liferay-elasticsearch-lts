// ### THIS SCRIPT IS EXPECTED TO BE RUN IN LIFERAY'S CONTROL PANEL SCRIPT
// ### MODULE
//
// ### YOU MAY HAVE TO UPDATE THE PATH TO YOUR JSON FILE. JUST UPDATE THE VALUE
// ### OF THE `MOVIES_FILE_PATH` VARIABLE BELOW

import com.liferay.portal.kernel.json.JSONFactoryUtil
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil
import com.liferay.blogs.service.BlogsEntryLocalServiceUtil
import com.liferay.portal.kernel.util.Validator;
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.time.LocalDateTime
import java.time.Duration

MOVIES_FILE_PATH = "/home/adam/software/elasticsearch-learning-to-rank/demo/tmdb.json"

def getServiceContext() {
 def groups = GroupLocalServiceUtil.getGroups(-1, -1);
 def group = null;
 for (g in groups) {
  if (g.getName(LocaleUtil.US) == "Guest") {
   group = g;
  }
 }
 def sc = ServiceContextThreadLocal.getServiceContext();
 sc.scopeGroupId = group.groupId;
 return sc;
}

def loadJSONObject(jsonFilePath) {
 System.err.println("Reading file");
 def fileContent = new File(jsonFilePath).getText('UTF-8')
 def data = JSONFactoryUtil.createJSONObject(fileContent);
 System.err.println(data.length())
 System.err.println("File read @ " + new Date())
 return data;
}

def getNonNull(String dft, String... strings) {
  for (string in strings) {
   if (Validator.isNotNull(string)) return string;
 }
 return dft
}

def getReleaseDate(dateStr) {
 try {
   return Date.from(LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant());
 } catch (Exception e) {
   return  new Date();
 }
}

String[] getTags(collectionData) {
 if (collectionData != null && Validator.isNotNull(collectionData.getString("name"))) {
  def collection = collectionData.getString("name");
  return [collection.replaceAll($/[\[\]&'@"}:,=>/<;*/~+#`?]/$, "")];
 }
 return []
}

def getMovie(key, movieJSON) {
 def title = getNonNull(key, movieJSON.getString("title"), movieJSON.getString("original_title"));
 return [
   title: title,
   content: getNonNull(title, movieJSON.getString("overview")),
   displayDate: getReleaseDate(movieJSON.getString("release_date")),
   tags: getTags(movieJSON.getJSONObject("belongs_to_collection"))
 ]
}

def importMovieAsBlogEntry(movie) {
 def sc = getServiceContext();
 def blogEntry = BlogsEntryLocalServiceUtil.addEntry(sc.userId, movie.title, movie.content, movie.displayDate, sc);
 if (movie.tags.length != 0) {
  System.err.println("Tagging " + movie.title + " with " + movie.tags[0]);
  entry = BlogsEntryLocalServiceUtil.updateAsset(sc.userId, blogEntry, new long[0], movie.tags, new long[0], 1.0);
 }
}

def startDateTime = LocalDateTime.now();
System.err.println("start importing @ " + startDateTime)

def data = loadJSONObject(MOVIES_FILE_PATH);
def count = 0
def total = data.length()
data.keys().each {
 key ->
  try {
   def movie = getMovie(key, data.getJSONObject(key))
   count = count + 1;
   System.err.println("Importing " + movie.title + " (" + count + " of " + total + ")");
   importMovieAsBlogEntry(movie);
  } catch (Exception e) {
   System.err.println("ERROR: " + e);
   System.err.println(count);
  }
}
def endDateTime = LocalDateTime.now()
System.err.println("import done @ " + endDateTime + "(" + Duration.between(endDateTime, startDateTime) + ")")

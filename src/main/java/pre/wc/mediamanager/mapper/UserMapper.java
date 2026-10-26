package pre.wc.mediamanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pre.wc.mediamanager.entity.User;

@Mapper
public interface UserMapper {

    int insert(User user);

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int updateLastLogin(@Param("id") Long id);
}

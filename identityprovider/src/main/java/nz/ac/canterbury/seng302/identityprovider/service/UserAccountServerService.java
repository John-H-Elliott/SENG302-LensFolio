package nz.ac.canterbury.seng302.identityprovider.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import nz.ac.canterbury.seng302.identityprovider.model.Roles;
import nz.ac.canterbury.seng302.identityprovider.model.UserModel;
import nz.ac.canterbury.seng302.shared.identityprovider.UserAccountServiceGrpc;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRegisterRequest;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRegisterResponse;
import nz.ac.canterbury.seng302.shared.util.FileUploadStatus;
import nz.ac.canterbury.seng302.shared.util.FileUploadStatusResponse;
import org.mariadb.jdbc.MariaDbBlob;
import org.springframework.beans.factory.annotation.Autowired;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.Objects;
import java.util.Set;

import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

import static nz.ac.canterbury.seng302.shared.util.FileUploadStatus.*;


@GrpcService
public class UserAccountServerService extends UserAccountServiceGrpc.UserAccountServiceImplBase {

    private Pbkdf2PasswordEncoder pbkdf2PasswordEncoder = new Pbkdf2PasswordEncoder();

    @Autowired
    private UserModelService userModelService;

    /***
     * Attempts to register a user with a given username, password, first name, middle name, last name, email.
     */
    @Override
    public void register(UserRegisterRequest request, StreamObserver<UserRegisterResponse> responseObserver) {
        UserRegisterResponse.Builder reply = UserRegisterResponse.newBuilder();

        boolean wasAdded = false;
        UserModel newUser;
        UserModel createdUser = null;

        UserModel uniqueUser = userModelService.getUserByUsername(request.getUsername());
        try {
            // Any empty fields are because you can't add those fields when you create an account initially.
            if (uniqueUser != null) {
                responseObserver.onNext(reply.setIsSuccess(false).build());
                responseObserver.onCompleted();
                return;
            }
            newUser = new UserModel(
                    request.getUsername(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getMiddleName(), //request.getMiddleName(),
                    request.getLastName(), //request.getLastName(),
                    "", //request.getNickname(),
                    request.getEmail(),
                    "Default Bio", //request.getBio(),
                    "Unknown Pronouns" //request.getPersonalPronouns()
            );
            createdUser = userModelService.addUser(newUser);
            wasAdded = true;
        } catch (Exception e) {
            System.err.println("Failed to create and add new user to database");
        }
        if (wasAdded) {
            responseObserver.onNext(reply.setNewUserId(createdUser.getUserId()).setIsSuccess(true).build());
        } else {
            responseObserver.onNext(reply.setIsSuccess(false).build());
        }
        responseObserver.onCompleted();
    }

    /***
     * Getter method to get user model with a given user ID
     */
    @Override
    public void getUserAccountById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {

        UserResponse.Builder reply = UserResponse.newBuilder();
        boolean isExist = userModelService.existsByUserId(request.getId());
        try {
            if (!isExist) {
                reply.setEmail("");
            } else {
                UserModel user = userModelService.getUserById(request.getId());

                String profileImagePath = "";
                try {
                    Blob imageBlob = user.getPhoto();
                    new File("src/main/resources/Images").mkdirs();
                    File imageFile = new File("src/main/resources/Images/profileImage");
                    FileOutputStream imageOutput = new FileOutputStream(imageFile);
                    if  (imageBlob != null) {
                        imageOutput.write(imageBlob.getBytes(1, (int) imageBlob.length()));
                        profileImagePath = imageFile.getAbsolutePath();
                    } else {
                        profileImagePath = "";
                    }
                    imageOutput.close();
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }

                reply
                        .setEmail(user.getEmail())
                        .setFirstName(user.getFirstName())
                        .setLastName(user.getLastName())
                        .setMiddleName(user.getMiddleName())
                        .setUsername(user.getUsername())
                        .setNickname(user.getNickname())
                        .setBio(user.getBio())
                        .setPersonalPronouns(user.getPersonalPronouns())
                        .setCreated(user.getDateAdded())
                        .setProfileImagePath(profileImagePath);
                Set<Roles> roles = user.getRoles();
                Roles[] rolesArray = roles.toArray(new Roles[roles.size()]);

                for (Roles value : rolesArray) {
                    reply.addRolesValue(value.getId());
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void editUser(EditUserRequest request, StreamObserver<EditUserResponse> responseObserver) {
        EditUserResponse.Builder reply = EditUserResponse.newBuilder();

        boolean wasSaved;

        try {
            UserModel currentUser = userModelService.getUserById(request.getUserId());
            UserModel user = new UserModel();
            user.setUserId(request.getUserId());
            user.setBio(request.getBio());
            user.setEmail(request.getEmail());
            user.setNickname(request.getNickname());
            user.setFirstName(request.getFirstName());
            user.setMiddleName(request.getMiddleName());
            user.setLastName(request.getLastName());
            user.setPersonalPronouns(request.getPersonalPronouns());
            user.setUsername(currentUser.getUsername());
            user.setPassword(currentUser.getPassword());
            user.setDateAdded(currentUser.getDateAdded());
            user.setRoles(currentUser.getRoles());
            user.setPhoto(currentUser.getPhoto());
            wasSaved = userModelService.saveEditedUser(user);
            if (wasSaved) {
                reply.setIsSuccess(true).setMessage("User Account is successfully updated!");
            } else {
                reply.setIsSuccess(false).setMessage("Something went wrong");
            }
        } catch (Exception e) {
            System.err.println("User failed to be changed to new values");
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();

    }

    @Override
    public void changeUserPassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        ChangePasswordResponse.Builder reply = ChangePasswordResponse.newBuilder();

        boolean wasSaved;
        try {
            UserModel currentUser = userModelService.getUserById(request.getUserId());
            if (pbkdf2PasswordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
                UserModel user = new UserModel();
                user.setUserId(request.getUserId());
                user.setBio(currentUser.getBio());
                user.setEmail(currentUser.getEmail());
                user.setNickname(currentUser.getNickname());
                user.setFirstName(currentUser.getFirstName());
                user.setMiddleName(currentUser.getMiddleName());
                user.setLastName(currentUser.getLastName());
                user.setPersonalPronouns(currentUser.getPersonalPronouns());
                user.setUsername(currentUser.getUsername());
                user.setPassword(request.getNewPassword());
                user.setDateAdded(currentUser.getDateAdded());
                user.setRoles(currentUser.getRoles());
                user.setPhoto(currentUser.getPhoto());
                wasSaved = userModelService.saveEditedUser(user);
                if (wasSaved) {
                    reply.setIsSuccess(true).setMessage("User password successfully updated!");
                } else {
                    reply.setIsSuccess(false).setMessage("Something went wrong");
                }
            } else {
                reply.setIsSuccess(false).setMessage("Current password was incorrect.");
            }
        } catch (Exception e) {
            System.err.println("User failed to be changed to new values");
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }


    /**
     * Saves the new profile photo to the user using bi-directional streams
     * @param responseObserver for telling the portfolio method the current status
     * @return A Stream Observer that saves photo data as it is given
     */
    @Override
    public StreamObserver<UploadUserProfilePhotoRequest> uploadUserProfilePhoto(StreamObserver<FileUploadStatusResponse> responseObserver) {

        return new StreamObserver<UploadUserProfilePhotoRequest>() {
            ByteArrayOutputStream imageArray = new ByteArrayOutputStream();
            FileUploadStatus fileUploadStatus = PENDING;
            boolean byteFailed = false;
            String message = "Byte uploading";
            int userId;
            String fileType;

            @Override
            public void onNext(UploadUserProfilePhotoRequest value) {
                FileUploadStatusResponse.Builder reply = FileUploadStatusResponse.newBuilder();

                if (value.hasMetaData()) {
                    userId = value.getMetaData().getUserId();
                    fileType = value.getMetaData().getFileType();
                    responseObserver.onNext(reply.setStatus(fileUploadStatus).setMessage("Got Metadata").build());
                } else {
                    try {
                        imageArray.write(value.getFileContent().toByteArray());
                        fileUploadStatus = IN_PROGRESS;
                        message = "Bytes uploading";
                        responseObserver.onNext(reply.setStatus(fileUploadStatus).setMessage(message).build());
                    } catch (IOException e) {
                        fileUploadStatus = FAILED;
                        message = "Bytes failed to write to OutputStream";
                        byteFailed = true;
                        responseObserver.onNext(reply.setStatus(fileUploadStatus).setMessage(message).build());
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Failed to stream image: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                FileUploadStatusResponse.Builder reply = FileUploadStatusResponse.newBuilder();

                //  Call the function savePhotoToUser to save the photo
                Blob blob = new MariaDbBlob(imageArray.toByteArray());
                boolean wasSaved = false;
                if (!byteFailed) {
                    wasSaved = savePhotoToUser(userId, blob);
                }

                if (wasSaved) {
                    message = "Image saved to database";
                    fileUploadStatus = SUCCESS;
                } else {
                    message = "Image failed to save to database";
                    fileUploadStatus = FAILED;
                }

                responseObserver.onNext(reply.setStatus(fileUploadStatus).setMessage(message).build());
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Saves a photo to a user in the database. Overwrites anything already saved.
     * @param userId Id of the user you want to add the photo to
     * @param photo Photo blob for saving
     * @return Whether the new photo was saved
     */
    private boolean savePhotoToUser(int userId, Blob photo) {
        boolean status;
        try{
            UserModel user = userModelService.getUserById(userId);
            if (user != null) {
                user.setPhoto(photo);
                status = userModelService.saveEditedUser(user);
            } else {
                status = false;
            }
        } catch(Exception e) {
            status = false;
            System.err.println("Photo not saved");
            e.printStackTrace();
        }
        return status;
    }

    /**
     * Deletes a users image from the database (Sets it to null).
     * @param request Holds the user Id
     * @param responseObserver Tells the portfolio the status of whether the photo was deleted
     */
    @Override
    public void deleteUserProfilePhoto(DeleteUserProfilePhotoRequest request, StreamObserver<DeleteUserProfilePhotoResponse> responseObserver) {
        DeleteUserProfilePhotoResponse.Builder reply = DeleteUserProfilePhotoResponse.newBuilder();

        boolean wasDeleted = false;
        String message = "Photo failed to delete";

        try {
            UserModel user = userModelService.getUserById(request.getUserId());
            if (user != null) {
                user.setPhoto(null);
                wasDeleted = userModelService.saveEditedUser(user);
                message = "Photo deleted successfully";
            }
        } catch (Exception e) {
            System.err.println("Something went wrong deleting the users photo");
        }

        reply.setIsSuccess(wasDeleted);
//        reply.setMessage(message);
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPaginatedUsers(GetPaginatedUsersRequest request, StreamObserver<PaginatedUsersResponse> responseObserver) {
        PaginatedUsersResponse.Builder reply = PaginatedUsersResponse.newBuilder();
        List<UserModel> allUsers = userModelService.findAllUser();
        for (UserModel allUser : allUsers) {
            reply.addUsers(getUserInfo(allUser));
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    private UserResponse getUserInfo(UserModel user) {
        UserResponse.Builder response = UserResponse.newBuilder();
        response.setUsername(user.getUsername())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setNickname(user.getNickname());
        Set<Roles> roles = user.getRoles();
        Roles[] rolesArray = roles.toArray(new Roles[roles.size()]);

        for (int i = 0; i < rolesArray.length; i++) {
            response.addRolesValue(rolesArray[i].getId());
        }
        return response.build();
    }

//    public void addRoleToUser(ModifyRoleOfUserRequest request) {
//        UserRoleChangeResponse.Builder reply = UserRoleChangeResponse.newBuilder();
//        reply.
//    }
}
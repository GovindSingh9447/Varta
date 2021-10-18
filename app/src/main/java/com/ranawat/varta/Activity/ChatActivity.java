package com.ranawat.varta.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranawat.varta.Adapters.MessageAdapter;
import com.ranawat.varta.Models.Message;
import com.ranawat.varta.R;
import com.ranawat.varta.databinding.ActivityChatBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;

    MessageAdapter adapter;
    ArrayList<Message> messages;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String sendtUid;
    String receiverUid;

    String senderRoom, receiverRoom;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image..");
        dialog.setCancelable(false);

        database =FirebaseDatabase.getInstance();
        storage= FirebaseStorage.getInstance();

        messages =new ArrayList<>();
        adapter =new MessageAdapter(this,messages,senderRoom,receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        String name = getIntent().getStringExtra("name");
        String profile = getIntent().getStringExtra("image");

        binding.name.setText(name);

        Glide.with(ChatActivity.this).load(profile)
                .placeholder(R.drawable.avatar)
                .into(binding.profileC);




        receiverUid =getIntent().getStringExtra("uid");
        sendtUid = FirebaseAuth.getInstance().getUid();


        senderRoom =sendtUid +receiverUid;
        receiverRoom=receiverUid + sendtUid;

        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    String status =snapshot.getValue(String.class);
                    if (!status.isEmpty()){

                       /* if(status.equals("offline"))
                        {binding.status.setVisibility(View.GONE);}else{*/

                        binding.status.setText(status);
                        binding.status.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





       database.getReference().child("chats")
               .child(senderRoom)
               .child("messages")
               .addValueEventListener(new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                       messages.clear();
                       for (DataSnapshot snapshot1 : snapshot.getChildren()){
                           Message message=snapshot1.getValue(Message.class);
                           message.setMessageId(snapshot1.getKey());
                           messages.add(message);
                       }
                       adapter.notifyDataSetChanged();
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError error) {

                   }
               });

         binding.back.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent= new Intent(ChatActivity.this,MainActivity.class);
                 startActivity(intent);

             }
         });
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTex = binding.messageBox.getText().toString();

                Date date =new Date();
                Message message=new Message(messageTex, sendtUid,date.getTime());
                binding.messageBox.setText("");

                String randomKey =database.getReference().push().getKey();

                HashMap<String, Object> lastMsgObj= new HashMap<>();



                lastMsgObj.put("lastMsg", message.getMessage());

                lastMsgObj.put("lastMsgTime", date.getTime());



                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });
                        HashMap<String, Object> lastmsgObj= new HashMap<>();

                        lastmsgObj.put("lastMsg", message.getMessage());

                        lastmsgObj.put("lastMsgTime", date.getTime());

                        database.getReference().child("chats").child(senderRoom).updateChildren(lastmsgObj);
                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastmsgObj);
                    }
                });
            }
        });
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 25);

            }
        });
         final Handler handler=new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                database.getReference().child("presence").child(sendtUid).setValue("typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStopedTyping,1000);
            }
            Runnable userStopedTyping =new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(sendtUid).setValue("Online");

                }
            };
        });

        getSupportActionBar().hide();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==25){
            if (data!= null){
                if (data.getData()!=null){
                    Uri selectedImage = data.getData();
                    Calendar calendar= Calendar.getInstance();
                    StorageReference reference= storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if (task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();



                                        String messageTex = binding.messageBox.getText().toString();

                                        Date date =new Date();


                                        Message message=new Message(messageTex, sendtUid,date.getTime());
                                        message.setMessage("Photo");
                                        message.setImageUrl(filePath);
                                        binding.messageBox.setText("");

                                        String randomKey =database.getReference().push().getKey();

                                        HashMap<String, Object> lastMsgObj= new HashMap<>();

                                        lastMsgObj.put("lastMsg", message.getMessage());

                                        lastMsgObj.put("lastMsgTime", date.getTime());

                                        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey)
                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                });
                                                HashMap<String, Object> lastmsgObj= new HashMap<>();

                                                lastmsgObj.put("lastMsg", message.getMessage());

                                                lastmsgObj.put("lastMsgTime", date.getTime());

                                                database.getReference().child("chats").child(senderRoom).updateChildren(lastmsgObj);
                                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastmsgObj);
                                            }
                                        });
                                        //Toast.makeText(ChatActivity.this, filePath, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });


                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId =FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId =FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");

    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}